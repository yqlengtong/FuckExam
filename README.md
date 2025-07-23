# FuckExam - 屏幕实时OCR识别与大模型问答助手

| 程序主界面 | 实机演示1 | 实机演示2 |
|:---:|:---:|:---:|
| ![程序主界面](https://github.com/user-attachments/assets/40f0cfae-228b-4d8a-9841-d0b3a4bf84ef) | ![实机演示1](https://github.com/user-attachments/assets/7a8cfcd8-3cbb-44ac-8f34-72cb18324706) | ![实机演示2](https://github.com/user-attachments/assets/060cb12b-8642-4665-a359-fce3b71339c3) |

**FuckExam** 是一款专为需要快速查询和理解屏幕内容的场景设计的安卓应用。它通过一个悬浮球常驻在屏幕上，用户点击悬浮球即可触发全屏截图，并利用本地ML Kit OCR技术识别图中文字，随后将识别结果发送给配置好的大语言模型（如GPT、Gemini等），最终以悬浮窗的形式展示问答结果。这款工具在在线学习、阅读外文资料、应对在线测试等场景下尤其有用。

## ✨ 核心功能

- **悬浮球快捷操作**：启动后，一个可随意拖动、贴边隐藏的悬浮球会出现在屏幕上，方便随时调用。
- **一键截图与OCR识别**：点击悬浮球，应用会自动进行全屏截图，并利用Google ML Kit的离线OCR功能，精准快速地识别屏幕上的所有中文和英文文本。
- **集成大语言模型**：支持配置多种大语言模型（通过API Key），将OCR识别出的文本作为问题，向AI提问。
- **悬浮窗展示结果**：从大模型获取的答案会以一个简洁的悬浮窗展示在屏幕上，方便用户在不离开当前应用的情况下查看信息。
- **高度可定制**：用户可以在主界面选择希望使用的大模型，并输入自己的API Key进行配置。

## 🚀 技术栈

- **UI**：`Material Design 3`，`ConstraintLayout`
- **核心框架**：`Android SDK`
- **文本识别**：`Google ML Kit Text Recognition for Chinese`
- **网络请求**：`OkHttp`
- **异步处理**：`Coroutines` (推测)
- **前台服务与悬浮窗**：`Foreground Service`, `SYSTEM_ALERT_WINDOW`

## 🛠️ 如何使用

1.  **克隆或下载项目**：
    ```bash
    git clone https://github.com/yqlengtong/FuckExam.git
    ```
2.  **配置**：
    - 在Android Studio中打开项目。
    - 打开应用，在主界面的下拉菜单中选择你想要使用的大模型。
    - 在下方的输入框中，填入你对应大模型的API Key。
    - 点击“保存配置”按钮。
3.  **启动服务**：
    - 点击“启动”按钮，应用会请求悬浮窗和屏幕录制权限。
    - 授权后，一个小的半透明圆点（悬浮球）会出现在屏幕上。
4.  **开始使用**：
    - 在任何需要识别和查询的界面，点击该悬浮球。
    - 应用将自动完成截图、识别、提问和展示答案的全过程。

## ⚠️ 所需权限

为了实现其核心功能，本应用需要以下权限：

- `SYSTEM_ALERT_WINDOW`: 用于显示悬浮球和答案悬浮窗。
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PROJECTION`: 用于在后台运行屏幕捕捉服务，这是Android 10及以上版本进行屏幕截图的必要条件。
- `POST_NOTIFICATIONS`: 用于在启动前台服务时显示一个常驻通知，以符合Android系统要求。

## 🤝 贡献

欢迎提交Pull Requests或Issues来改进这个项目。

## 📄 许可证

该项目采用 [MIT License](LICENSE) 授权。 <!-- 如果您没有LICENSE文件，可以考虑添加一个 -->
