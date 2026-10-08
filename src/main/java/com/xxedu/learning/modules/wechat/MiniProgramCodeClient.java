package com.xxedu.learning.modules.wechat;

/**
 * 生成打开指定小程序页面的码图（PNG）。
 */
public interface MiniProgramCodeClient {

    byte[] createUnlimitedCode(String scene, String page, String envVersion);
}
