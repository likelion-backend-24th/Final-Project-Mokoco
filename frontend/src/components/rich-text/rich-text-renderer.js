import "./rich-text.css";

export default function RichTextRenderer({ content, contentFormat, className = "" }) {
  if (contentFormat !== "HTML") {
    return <p className={`whitespace-pre-line ${className}`}>{content}</p>;
  }
  return <div className={`rich-text-content ${className}`} dangerouslySetInnerHTML={{ __html: content }} />;
}
