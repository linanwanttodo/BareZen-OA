package com.linan.system.service.impl;

import com.linan.system.service.IDbBackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 数据库备份 Service 实现
 *
 * <p>通过 mysqldump 命令实现 MySQL 数据库备份。</p>
 *
 * @author BareZen Team
 */
@Slf4j
@Service
public class DbBackupServiceImpl implements IDbBackupService {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username:root}")
    private String dbUser;

    @Value("${spring.datasource.password:}")
    private String dbPassword;

    /** 有界异步线程池，用于消费 mysqldump 的 stderr，避免裸 new Thread 脱离线程管理 */
    @Autowired
    @Qualifier("asyncTaskExecutor")
    private Executor asyncTaskExecutor;

    /** 备份文件存储目录 */
    private static final String BACKUP_DIR = System.getProperty("user.dir") + "/backup/";

    /** 备份保留天数 */
    private static final int RETENTION_DAYS = 30;

    /** mysqldump 执行超时（分钟），避免进程挂起时调用线程永久阻塞 */
    private static final long BACKUP_TIMEOUT_MINUTES = 10;

    @Override
    public Map<String, Object> createBackup() {
        Map<String, Object> result = new HashMap<>();
        // 临时 my.cnf 配置文件路径
        Path tempCnfPath = null;
        try {
            // 确保备份目录存在
            File dir = new File(BACKUP_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 解析数据库名称
            String dbName = parseDbName(dbUrl);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = dbName + "_" + timestamp + ".sql";
            String filePath = BACKUP_DIR + filename;

            // 创建临时 my.cnf 配置文件，避免密码通过命令行参数暴露
            tempCnfPath = createTempMyCnfFile(dbUser, dbPassword);

            // 拼接 mysqldump 命令
            List<String> command = new ArrayList<>();
            command.add("mysqldump");
            command.add("--defaults-extra-file=" + tempCnfPath.toAbsolutePath());
            command.add("-h" + parseDbHost(dbUrl));
            command.add("-P" + parseDbPort(dbUrl));
            command.add("--routines");
            command.add("--single-transaction");
            command.add("--set-charset=utf8mb4");
            command.add(dbName);

            // 执行备份：stdout 写入备份文件，stderr 单独读取记录日志
            // （不能 redirectErrorStream，否则警告/错误文本会混入 SQL 文件导致备份损坏）
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(new File(filePath));
            Process process = pb.start();

            // 异步消费 stderr，避免缓冲区写满导致进程阻塞；
            // 改用有界线程池（asyncTaskExecutor）替代裸 new Thread，纳入统一线程管理，
            // 通过 CountDownLatch 等待消费完成
            StringBuilder errorOutput = new StringBuilder();
            CountDownLatch errDone = new CountDownLatch(1);
            asyncTaskExecutor.execute(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorOutput.append(line).append('\n');
                    }
                } catch (IOException e) {
                    log.warn("读取 mysqldump 错误输出失败", e);
                } finally {
                    errDone.countDown();
                }
            });

            // 带超时等待，超时后强制终止子进程
            boolean finished = process.waitFor(BACKUP_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                result.put("success", false);
                result.put("message", "mysqldump 执行超时（" + BACKUP_TIMEOUT_MINUTES + " 分钟），已强制终止");
                log.error("数据库备份超时，已强制终止 mysqldump 进程");
                Files.deleteIfExists(Paths.get(filePath));
                return result;
            }
            errDone.await(5, TimeUnit.SECONDS);
            int exitCode = process.exitValue();

            if (exitCode == 0) {
                File backupFile = new File(filePath);
                result.put("success", true);
                result.put("filename", filename);
                result.put("fileSize", backupFile.length());
                result.put("createTime", LocalDateTime.now().toString());
                log.info("数据库备份成功: {} ({} bytes)", filename, backupFile.length());

                // 清理过期备份
                cleanOldBackups();
            } else {
                result.put("success", false);
                result.put("message", "mysqldump 执行失败，退出码: " + exitCode);
                log.warn("数据库备份失败，退出码: {}, stderr: {}", exitCode, errorOutput);
                // 删除失败产生的不完整备份文件
                Files.deleteIfExists(Paths.get(filePath));
            }
        } catch (Exception e) {
            log.error("数据库备份异常", e);
            result.put("success", false);
            // getMessage() 可能为 null，兜底使用异常类型名，避免前端展示 "null"
            String msg = e.getMessage();
            result.put("message", msg != null ? msg : e.getClass().getSimpleName());
        } finally {
            // 执行完毕后立即删除临时 my.cnf 配置文件
            if (tempCnfPath != null) {
                try {
                    Files.deleteIfExists(tempCnfPath);
                    log.debug("已删除临时配置文件: {}", tempCnfPath);
                } catch (IOException e) {
                    log.warn("删除临时配置文件失败: {}", tempCnfPath, e);
                }
            }
        }
        return result;
    }

    /**
     * 创建临时 my.cnf 配置文件，用于存放数据库凭据。
     * 文件权限设置为 0600（仅所有者可读写），执行完毕后由调用方删除。
     *
     * @param user     数据库用户名
     * @param password 数据库密码
     * @return 临时文件的路径
     * @throws IOException 如果文件创建或写入失败
     */
    private Path createTempMyCnfFile(String user, String password) throws IOException {
        Path tempFile = Files.createTempFile("mysql_backup_", ".cnf");
        // 设置文件权限为 0600（仅所有者可读写）
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(tempFile, perms);
        } catch (Exception e) {
            // 非 POSIX 文件系统（如 Windows）不支持 PosixFilePermission，忽略该异常
            log.debug("设置文件权限失败（非 POSIX 文件系统）: {}", e.getMessage());
        }
        // 写入 my.cnf 内容
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(tempFile.toFile()), StandardCharsets.UTF_8)) {
            writer.write("[client]\n");
            writer.write("user = " + user + "\n");
            writer.write("password = " + password + "\n");
            writer.flush();
        }
        log.debug("已创建临时配置文件: {}", tempFile);
        return tempFile;
    }

    @Override
    public List<Map<String, Object>> listBackups() {
        List<Map<String, Object>> backups = new ArrayList<>();
        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) {
            return backups;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));
        if (files == null) {
            return backups;
        }

        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

        for (File file : files) {
            Map<String, Object> info = new HashMap<>();
            info.put("filename", file.getName());
            info.put("fileSize", file.length());
            info.put("createTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date(file.lastModified())));
            backups.add(info);
        }
        return backups;
    }

    /**
     * 校验文件名是否安全（防止路径遍历）
     *
     * @param filename 用户传入的文件名
     * @return 规范化后的安全文件对象，若不安全返回 null
     */
    private File safeBackupFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        // 仅允许文件名，禁止路径分隔符
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            log.warn("检测到路径遍历尝试: {}", filename);
            return null;
        }
        if (!filename.endsWith(".sql")) {
            return null;
        }
        File file = new File(BACKUP_DIR + filename);
        try {
            // 验证规范化后的路径仍在 BACKUP_DIR 范围内
            String canonical = file.getCanonicalPath();
            String baseDir = new File(BACKUP_DIR).getCanonicalPath();
            if (!canonical.startsWith(baseDir + File.separator) && !canonical.equals(baseDir + filename)) {
                log.warn("路径遍历检测失败: canonical={}, baseDir={}", canonical, baseDir);
                return null;
            }
        } catch (IOException e) {
            log.error("路径规范化异常", e);
            return null;
        }
        return file.exists() ? file : null;
    }

    @Override
    public boolean deleteBackup(String filename) {
        File file = safeBackupFile(filename);
        if (file != null) {
            return file.delete();
        }
        return false;
    }

    @Override
    public String getBackupPath(String filename) {
        File file = safeBackupFile(filename);
        if (file != null) {
            return file.getAbsolutePath();
        }
        return null;
    }

    /**
     * 清理超过保留天数的备份文件
     */
    private void cleanOldBackups() {
        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) return;

        long cutoff = System.currentTimeMillis() - RETENTION_DAYS * 24L * 3600 * 1000;
        File[] files = dir.listFiles((d, name) -> name.endsWith(".sql"));
        if (files == null) return;

        for (File file : files) {
            if (file.lastModified() < cutoff) {
                if (file.delete()) {
                    log.info("已清理过期备份: {}", file.getName());
                }
            }
        }
    }

    private String parseDbName(String url) {
        int idx = url.indexOf('?');
        String clean = idx > 0 ? url.substring(0, idx) : url;
        int lastSlash = clean.lastIndexOf('/');
        return lastSlash > 0 ? clean.substring(lastSlash + 1) : "barezen_oa";
    }

    private String parseDbHost(String url) {
        String hostPort = url.split("://")[1].split("/")[0];
        return hostPort.split(":")[0];
    }

    private String parseDbPort(String url) {
        String hostPort = url.split("://")[1].split("/")[0];
        String[] parts = hostPort.split(":");
        return parts.length > 1 ? parts[1] : "3306";
    }
}
