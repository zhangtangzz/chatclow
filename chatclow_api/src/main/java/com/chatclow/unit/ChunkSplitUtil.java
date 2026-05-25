package com.chatclow.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本切片工具
 * 将长文本按固定大小切分成多个片段，尽量在句子边界处切分
 */
public class ChunkSplitUtil {

    /**
     * 默认切片大小（字符数）
     */
    private static final int DEFAULT_CHUNK_SIZE = 500;

    /**
     * 按默认大小（500字）切片
     */
    public static List<String> split(String text) {
        return split(text, DEFAULT_CHUNK_SIZE);
    }

    /**
     * 按指定大小切片，尽量在句子边界处切分
     * @param text 原始文本
     * @param chunkSize 每块最大字符数
     * @return 切片列表
     */
    public static List<String> split(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        int textLength = text.length();

        while (start < textLength) {
            // 如果剩余文本小于 chunkSize，直接加入
            if (start + chunkSize >= textLength) {
                chunks.add(text.substring(start).trim());
                break;
            }

            // 在 chunkSize 附近找句子边界
            int end = findSentenceBoundary(text, start, start + chunkSize);
            chunks.add(text.substring(start, end).trim());
            start = end;
        }

        return chunks;
    }

    /**
     * 在 [start, preferredEnd] 范围内找句子边界
     * 优先在。！？\n 处切分
     * 如果找不到，就在 preferredEnd 处硬切
     */
    private static int findSentenceBoundary(String text, int start, int preferredEnd) {
        // 从 preferredEnd 往前找，最多往前找 150 个字符
        int searchStart = Math.max(start, preferredEnd - 150);

        for (int i = preferredEnd; i >= searchStart; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '\n' || c == ';' || c == '；') {
                return i + 1; // 切在句子结束符之后
            }
        }

        // 没找到句子边界，就在 preferredEnd 处硬切
        return preferredEnd;
    }
}
