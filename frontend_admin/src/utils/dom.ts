/**
 * HTML 消毒 —— 防止 XSS
 *
 * 使用 DOMPurify 取代原有正则实现，提供基于真实 DOM 解析的消毒能力。
 * DOMPurify 经过广泛的安全审计，能有效防御各种 XSS 绕过技巧。
 *
 * @see https://github.com/cure53/DOMPurify
 */
import DOMPurify from 'dompurify'

// 对带 target 的链接强制加 rel="noopener noreferrer"，防止反向标签劫持（reverse tabnabbing）
DOMPurify.addHook('afterSanitizeAttributes', (node) => {
  if (node.tagName === 'A') {
    if (node.hasAttribute('target')) {
      node.setAttribute('rel', 'noopener noreferrer')
    }
  }
})

/**
 * 对 HTML 字符串进行消毒，移除危险标签和属性
 * @param html 原始 HTML 字符串
 * @returns 消毒后的安全 HTML
 */
export function sanitizeHtml(html: string): string {
  if (!html) return ''
  return DOMPurify.sanitize(html, {
    ALLOWED_TAGS: [
      'b', 'i', 'u', 'em', 'strong', 'a', 'br', 'p',
      'ul', 'ol', 'li', 'span', 'div', 'h1', 'h2', 'h3',
      'h4', 'h5', 'h6', 'blockquote', 'pre', 'code', 'hr',
      'table', 'thead', 'tbody', 'tr', 'th', 'td',
      'img', 'figure', 'figcaption',
    ],
    // 不允许 id（DOM clobbering 风险）；富文本排版保留 style/class
    ALLOWED_ATTR: [
      'href', 'target', 'rel', 'title', 'alt', 'src',
      'width', 'height', 'class', 'style',
      'align', 'valign', 'colspan', 'rowspan',
    ],
    // 限制 URL 协议：仅 http/https/mailto 与相对路径（另允许 data:image 图片）
    ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|data:image\/(?:png|jpe?g|gif|webp|svg\+xml);|[^a-z]|[a-z+.-]+(?:[^a-z+.\-:]|$))/i,
    ALLOW_DATA_ATTR: false,
  })
}
