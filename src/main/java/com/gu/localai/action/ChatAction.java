package com.gu.localai.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;

import javax.swing.*;

/**
 * @description: todo
 * @author: guhuanqi
 * @create: 2025-03-06 15:57
 **/
public class ChatAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        JFrame frame = new JFrame("Ollama Chat");
        frame.setContentPane(new ChatPanel());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}