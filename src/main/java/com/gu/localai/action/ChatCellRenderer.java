package com.gu.localai.action;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @description: todo
 * @author: guhuanqi
 * @create: 2025-03-06 15:56
 **/
class ChatCellRenderer extends JPanel implements ListCellRenderer<ChatMessage> {
    private final JList<ChatMessage> chatList; // 新增字段
    private final JTextPane textPane = new JTextPane();
    private final JLabel timeLabel = new JLabel();

    public ChatCellRenderer(JList<ChatMessage> chatList) {
        this.chatList = chatList;
        setLayout(new BorderLayout());
        setOpaque(true);

        // 文本区域配置
        textPane.setEditable(false);
        textPane.setContentType("text/html");
        textPane.setOpaque(false);

        // 时间标签样式
        timeLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        timeLabel.setForeground(Color.GREEN);

        add(textPane, BorderLayout.CENTER);
        add(timeLabel, BorderLayout.SOUTH);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends ChatMessage> list,
            ChatMessage msg,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        // 转义HTML特殊字符
        String safeContent = msg.content
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");
        // 动态计算可用宽度
        int maxWidth = Math.max(chatList.getWidth() - 70, 100); // 确保最小宽度

        // 构建带样式的HTML内容
        String htmlContent = String.format(
                "<html><body style='margin:3px; font-family:Microsoft YaHei, sans-serif;'>" +
                        "<div style='width:%dpx; word-wrap:break-word; color:#c8ee6a;'>%s</div>" +
                        "</body></html>",
                maxWidth,
                safeContent
        );

        textPane.setText(htmlContent);
        // 时间显示
        timeLabel.setText(new SimpleDateFormat("HH:mm:ss").format(new Date()));
        timeLabel.setHorizontalAlignment("user".equals(msg.role) ? SwingConstants.RIGHT : SwingConstants.LEFT);

        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        if (chatList == null || chatList.getWidth() <= 0) {
            return new Dimension(200, 40); // 确保最小显示高度
        }

        int availableWidth = calculateAvailableWidth();
        textPane.setSize(new Dimension(availableWidth, Short.MAX_VALUE));

        int textHeight = textPane.getPreferredSize().height;
        return new Dimension(
                availableWidth,
                Math.max(textHeight + 30, 40) // 最小40px高度
        );
    }

    private int calculateAvailableWidth() {
        // 计算可用宽度（考虑垂直滚动条）
        int listWidth = chatList.getWidth();
        boolean hasScrollBar = chatList.getParent() instanceof JViewport
                && ((JViewport)chatList.getParent()).getParent() instanceof JScrollPane
                && ((JScrollPane)((JViewport)chatList.getParent()).getParent())
                .getVerticalScrollBar().isVisible();

        return listWidth - (hasScrollBar ? 35 : 20);
    }
}