# 英汉电子词典

一个基于 JavaFX + Maven 的英汉电子词典课程项目，支持本地词库查询、联想建议、历史记录、收藏功能，以及本地未命中时的在线补查。

## 项目结构

```text
English-Chinese Electronic Dictionary/
├─ code/        源代码、Maven 配置、打包脚本
├─ bin/         当前可直接运行的程序目录
├─ doc/         阶段规划、实现说明、使用记录
└─ README.md    项目总说明
```

## 功能概览

- 本地词库优先查询
- 英文和中文关键词双向检索
- 输入联想建议
- 查询历史与收藏
- 本地未命中时在线补查
- 支持打包为 Windows 桌面应用

## 运行环境

- JDK 21
- Maven 3.9 及以上
- Windows 10/11

## 本地运行

进入代码目录：

```powershell
cd "E:\Study\codes\English-Chinese Electronic Dictionary\code"
```

编译项目：

```powershell
mvn "-Dmaven.repo.local=.m2" clean compile
```

运行桌面界面：

```powershell
mvn "-Dmaven.repo.local=.m2" javafx:run
```

## 图标替换说明

图标应该在打包前替换，不建议打包成 `exe` 后再改。

原因很简单：

- 打包前替换，`jpackage` 会把图标写进生成的桌面应用和安装包
- 打包后再改 `exe`，通常需要额外的 PE 资源编辑工具，不稳定，也容易被系统缓存旧图标

请把 Windows 图标文件放到这个位置：

[`code/assets/app-icon.ico`](/E:/Study/codes/English-Chinese%20Electronic%20Dictionary/code/assets/app-icon.ico)

当前打包脚本已经默认读取这个路径：

[`code/scripts/package-windows.ps1`](/E:/Study/codes/English-Chinese%20Electronic%20Dictionary/code/scripts/package-windows.ps1)

注意：

- 文件名建议直接固定为 `app-icon.ico`
- 推荐使用 `.ico` 格式
- 最好包含 `256x256`、`128x128`、`64x64`、`48x48`、`32x32` 多个尺寸

## Windows 打包

进入代码目录：

```powershell
cd "E:\Study\codes\English-Chinese Electronic Dictionary\code"
```

生成应用目录版：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -Type app-image
```

生成 `exe` 安装包：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-windows.ps1 -Type exe
```

生成后的文件默认在：

[`code/target/dist`](/E:/Study/codes/English-Chinese%20Electronic%20Dictionary/code/target/dist)

## 两种交付包怎么分

建议分成两个压缩包，不要混在一起：

### 1. 源码包

用于老师查看项目结构、代码、文档。

建议包含：

- `code/`
- `doc/`
- `README.md`

建议不要包含：

- `code/target/`
- `code/.m2/`
- 运行时临时缓存

推荐做法：

1. 在项目根目录新建一个干净目录，例如 `release/source`
2. 复制 `code`、`doc`、`README.md` 到 `release/source`
3. 删除 `release/source/code/target`
4. 把 `release/source` 压缩成 `english-chinese-dictionary-source.zip`

### 2. 可运行程序包

用于直接双击运行或安装。

如果你要交“免安装版”，打 `app-image`：

- 压缩 [`code/target/dist/EnglishChineseDictionary`](/E:/Study/codes/English-Chinese%20Electronic%20Dictionary/code/target/dist/EnglishChineseDictionary)

如果你要交“安装版”，打 `exe`：

- 提交 [`code/target/dist`](/E:/Study/codes/English-Chinese%20Electronic%20Dictionary/code/target/dist) 里生成的 `.exe`

推荐命名：

- `english-chinese-dictionary-source.zip`
- `english-chinese-dictionary-exe.zip` 或 `english-chinese-dictionary-setup.exe`

## 推荐交付流程

1. 先把图标放到 `code/assets/app-icon.ico`
2. 运行打包脚本生成 `app-image` 或 `exe`
3. 检查程序能否正常启动
4. 单独整理源码包
5. 单独整理可执行程序包

## 目前重要文件

- 主界面入口：[`code/src/main/java/com/ecdictionary/DictionaryApplication.java`](/E:/Study/codes/English-Chinese%20Electronic%20Dictionary/code/src/main/java/com/ecdictionary/DictionaryApplication.java)
- Maven 配置：[`code/pom.xml`](/E:/Study/codes/English-Chinese%20Electronic%20Dictionary/code/pom.xml)
- Windows 打包脚本：[`code/scripts/package-windows.ps1`](/E:/Study/codes/English-Chinese%20Electronic%20Dictionary/code/scripts/package-windows.ps1)
- 第三阶段规划：[`doc/第三阶段任务规划.md`](/E:/Study/codes/English-Chinese%20Electronic%20Dictionary/doc/第三阶段任务规划.md)

## 已完成的本次整理

- 删除了主标题下方说明小字
- 删除了结果区下方“第二阶段亮点”小字
- 整理了主界面与在线查询相关中文文案编码
- 补充了根目录 README

## 已知说明

- 在线补查依赖外部接口，网络异常时可能失败
- `jpackage` 需要 JDK 21 环境
- 若 Windows 图标没有立刻刷新，可能是系统图标缓存未更新，重新生成安装包后通常即可恢复正常
