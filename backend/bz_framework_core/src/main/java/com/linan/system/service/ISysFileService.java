package com.linan.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.linan.system.entity.SysFile;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ISysFileService extends IService<SysFile> {
    /** 上传文件到指定文件夹（上传人由登录态自动填充） */
    SysFile uploadFile(MultipartFile file, Long folderId, String scope) throws IOException;

    /**
     * 上传文件到指定文件夹并显式指定上传人
     *
     * <p>用户端口径：上传人强制为当前登录用户，接口不信任前端传入的身份。
     * 传 null 时与三参重载一致，由登录态自动填充 create_by。</p>
     */
    SysFile uploadFile(MultipartFile file, Long folderId, String scope, Long uploaderId) throws IOException;

    /** 下载文件 */
    void downloadFile(Long id, HttpServletResponse response);

    /** 删除文件或文件夹（递归删除子文件/子文件夹） */
    boolean deleteFile(Long id);

    /** 创建文件夹 */
    SysFile createFolder(String folderName, Long parentId, String scope);

    /** 重命名文件夹或文件 */
    SysFile rename(Long id, String newName);

    /** 获取文件夹树 */
    List<SysFile> getFolderTree(Long parentId);

    /** 获取指定文件夹下的子文件+子文件夹（分页） */
    IPage<SysFile> pageByFolder(Page<SysFile> page, Long folderId, String scope, String fileType, String originalName);

    /** 获取子文件夹列表（不分页） */
    List<SysFile> getSubFolders(Long folderId);
}
