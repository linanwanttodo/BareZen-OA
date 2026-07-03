/**
 * 操作列批量对齐 TDesign Starter（list/base）
 *
 * 官方基准：<t-space><t-link theme="primary">详情</t-link><t-link theme="danger">删除</t-link></t-space>
 * 本项目现状：操作列用 <t-button size="small"> 实心/描边按钮，30+ 个页面共 300+ 个，与模板站观感差异最大。
 *
 * 安全边界（刻意收窄，避免误伤）：
 *   1. 只处理「表格列插槽」内的 t-button —— 即位于 <template #xxx="{ ... }"> ... </template> 中的按钮；
 *      工具条（.bz-list-toolbar）、筛选区、表单弹窗里的 t-button 一律不动。
 *   2. <t-popconfirm> 内包裹的按钮保持 t-button —— popconfirm 需要真实按钮做触发器锚点，
 *      换成 t-link 会丢失点击热区与 hover/focus 锚点。
 *   3. 带 loading / disabled 动态态的按钮保持 t-button —— 异步操作需要可见的进度反馈。
 *
 * 语义映射：theme="danger" 保留 danger；其余统一 primary。
 *
 * 一次性工具：转换后必须跑 vite build 验证
 * （vue-tsc 与 vitest 都不校验模板闭合性，只有构建期会报 Invalid end tag）。
 */
import fs from 'node:fs';
import path from 'node:path';

const VIEWS = path.resolve(process.cwd(), 'src/views');

// 表格列插槽：#operate / #op / #operation / #col_N / #actions 等
const SLOT_RE = /<template\s+#(col_\d+|operate|operation|op|actions?)\s*=\s*"\{[^"]*"\s*>/g;
const CLOSE_TAG = '</template>';

function convertFile(file) {
  const src = fs.readFileSync(file, 'utf8');
  const orig = src;

  const ranges = [];
  SLOT_RE.lastIndex = 0;
  let m;
  while ((m = SLOT_RE.exec(src))) {
    const start = m.index;
    const end = src.indexOf(CLOSE_TAG, start);
    if (end === -1) continue;
    ranges.push([start, end + CLOSE_TAG.length]);
  }
  if (!ranges.length) return { changed: false, count: 0 };

  let out = '';
  let cursor = 0;
  let count = 0;

  for (const [start, end] of ranges) {
    out += src.slice(cursor, start);
    let seg = src.slice(start, end);

    // 1) t-popconfirm 内的按钮保持原样：整段抠出来暂存
    const popBlocks = [];
    seg = seg.replace(/<t-popconfirm[\s\S]*?<\/t-popconfirm>/g, (blk) => {
      popBlocks.push(blk);
      return `@@POP${popBlocks.length - 1}@@`;
    });

    // 2) 剩余部分的 t-button → t-link
    //    非自闭合分支必须同时替换配对的 </t-button>，否则模板标签不闭合
    seg = seg.replace(/<t-button\b([^>]*?)(\/>|>[\s\S]*?<\/t-button>)/g, (full, attrs, tail) => {
      if (/[:@]?(loading|disabled)\b/.test(attrs)) return full;
      if (/\bloading=/.test(attrs)) return full;

      let a = attrs;
      const themeMatch = a.match(/\stheme="([^"]+)"/);
      let theme = 'primary';
      if (themeMatch) {
        theme = themeMatch[1] === 'danger' ? 'danger' : 'primary';
        a = a.replace(/\stheme="[^"]+"/, '');
      }

      a = a
        .replace(/\ssize="[^"]*"/g, '')
        .replace(/\svariant="[^"]*"/g, '')
        .replace(/\sshape="[^"]*"/g, '')
        .replace(/\shtml-type="[^"]*"/g, '')
        .replace(/\sloading="[^"]*"/g, '')
        .replace(/\s+/g, ' ')
        .trim();

      count += 1;
      // tail 以 /> 开头为自闭合；否则把配对的 </t-button> 换成 </t-link>
      const closing = tail.startsWith('/>') ? '/>' : tail.replace(/<\/t-button>\s*$/, '</t-link>');
      return `<t-link theme="${theme}"${a ? ' ' + a : ''}${closing}`;
    });

    // 3) 还原 popconfirm 块
    seg = seg.replace(/@@POP(\d+)@@/g, (_, i) => popBlocks[Number(i)]);

    out += seg;
    cursor = end;
  }
  out += src.slice(cursor);

  if (out === orig) return { changed: false, count: 0 };
  fs.writeFileSync(file, out, 'utf8');
  return { changed: true, count };
}

let totalFiles = 0;
let totalButtons = 0;

function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(p);
    else if (entry.name.endsWith('.vue')) {
      const r = convertFile(p);
      if (r.changed) {
        totalFiles += 1;
        totalButtons += r.count;
        console.log(`${path.relative(VIEWS, p)}  (${r.count})`);
      }
    }
  }
}

walk(VIEWS);
console.log(`\nconverted files: ${totalFiles}, buttons: ${totalButtons}`);
