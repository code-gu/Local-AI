package com.gu.localai.action;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @description: todo
 * @author: guhuanqi
 * @create: 2025-03-06 15:48
 **/
public class ChatPanel extends JPanel {
    private final DefaultListModel<ChatMessage> chatModel = new DefaultListModel<>();
    private final JList<ChatMessage> chatList = new JList<>(chatModel);
    private final JTextField inputField = new JTextField(40);
    private final JButton sendButton = new JButton("Send");
    private final List<Map<String, String>> chatHistory = new ArrayList<>();

    public ChatPanel() {
        setLayout(new BorderLayout(0, 10)); // 整体使用边界布局
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chatList.setCellRenderer(new ChatCellRenderer(chatList));
        // 聊天列表区域
        chatList.setLayoutOrientation(JList.VERTICAL);
        JScrollPane listScrollPane = new JScrollPane(chatList);
        listScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(listScrollPane, BorderLayout.CENTER);

        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // 动态尺寸调整
        chatList.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                chatList.setFixedCellWidth(chatList.getWidth() - 25);
            }
        });
        // 在构造函数中添加输入框监听
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {

            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                adjustInputHeight();
            }

            private void adjustInputHeight() {
                int rows = inputField.getColumns();
                inputField.setColumns(Math.min(Math.max(rows, 1), 5)); // 限制1-5行高度
                inputField.revalidate();
            }
        });

        // 发送按钮事件绑定
        sendButton.addActionListener(e -> sendMessage());

        // 输入框回车键监听
        inputField.addActionListener(e -> sendMessage());

        // 全局回车键监听（备用方案）
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    if (e.getID() == KeyEvent.KEY_PRESSED
                            && e.getKeyCode() == KeyEvent.VK_ENTER
                            && inputField.isFocusOwner()) {
                        sendMessage();
                        return true;
                    }
                    return false;
                });

    }

    private void sendMessage() {
        String userInput = inputField.getText().trim();
        if (userInput.isEmpty()) return;

        // 立即显示用户消息
        addMessage("user", userInput);
        scrollToBottom(); // 立即滚动到用户消息位置
        inputField.setText("");

        new Thread(() -> {
            // 先添加空助手消息占位
            ChatMessage assistantMsg = new ChatMessage("assistant", "");
            SwingUtilities.invokeLater(() -> {
                chatModel.addElement(assistantMsg);
                chatList.ensureIndexIsVisible(chatModel.size()-1);
            });

            callOllamaStreaming(userInput, assistantMsg);
        }).start();
        // 在sendMessage方法中添加视觉反馈
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.RED, 2),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        Timer timer = new Timer(1000, e -> {
            inputField.setBorder(UIManager.getBorder("TextField.border"));
        });
        timer.setRepeats(false);
        timer.start();
    }

    private synchronized void addMessage(String role, String content) {
        System.out.println("Adding message: [" + role + "] " + content); // 调试输出
        ChatMessage msg = new ChatMessage(role, content);
        chatModel.addElement(msg);

        // 保持最多100条消息
        if (chatModel.size() > 100) {
            chatModel.remove(0);
        }

        // 触发重绘
        chatList.revalidate();
        chatList.repaint();
    }

    // 流式响应处理将在后续实现


    private void callOllamaStreaming(String userInput, ChatMessage assistantMsg) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("http://localhost:11434/api/chat");
            post.setHeader("Content-Type", "application/json; charset=utf-8");

            // 构建带最新输入的请求体
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "deepseek-r1:1.5b");
            requestBody.addProperty("stream", true);
            System.out.println("输入:" + userInput);
            JsonArray messages = buildMessageArray(userInput);
            requestBody.add("messages", messages);



            post.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                InputStream content = response.getEntity().getContent();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(content, StandardCharsets.UTF_8)
                );

                // 修改流式处理部分
                StringBuilder assistantResponse = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty()) continue;

                    JsonObject json = JsonParser.parseString(line).getAsJsonObject();
                    if (json.has("message")) {
                        String delta = json.getAsJsonObject("message")
                                .get("content").getAsString();
                        assistantResponse.append(delta);

                        // 更新占位消息内容
                        assistantMsg.content = assistantResponse.toString();
                        SwingUtilities.invokeLater(() -> {
                            int index = chatModel.indexOf(assistantMsg);
                            chatModel.set(index, assistantMsg); // 触发重绘
                            scrollToBottom(); // 每次更新都触发滚动
                        });
                    }
                }

                // 最终保存完整响应
                chatHistory.add(Map.of("role", "assistant", "content", assistantResponse.toString()));
            }
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()));
        }
    }

    private JsonArray buildMessageArray(String latestInput) {
        JsonArray array = new JsonArray();

        // 添加历史记录
        for (Map<String, String> msg : chatHistory) {
            JsonObject obj = new JsonObject();
            obj.addProperty("role", msg.get("role"));
            obj.addProperty("content", msg.get("content"));
            array.add(obj);
        }

        // 显式添加最新输入（双保险）
        JsonObject latestMsg = new JsonObject();
        latestMsg.addProperty("role", "user");
        latestMsg.addProperty("content", latestInput);
        array.add(latestMsg);

        return array;
    }

    /**
     * 自动滚屏
     */
    private void scrollToBottom() {
        EventQueue.invokeLater(() -> {
            int lastIndex = chatModel.getSize() - 1;
            if (lastIndex >= 0) {
                // 确保UI更新完成
                chatList.updateUI();

                // 两种滚动方式确保到位
                chatList.ensureIndexIsVisible(lastIndex);

                // 直接控制滚动条
                JScrollBar vertical = ((JScrollPane)chatList.getParent().getParent())
                        .getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
    }
}