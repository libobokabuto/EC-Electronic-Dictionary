英汉电子词典软件运行说明
==========================

1. 如果 bin 目录中已经存在 english-chinese-dictionary.jar 和 lib 目录，可直接双击 run.bat 启动。
2. 如果运行文件不存在，请进入 code 目录执行：

   mvn clean package

3. 打包成功后，将以下内容复制到本目录：

   code\target\english-chinese-dictionary-1.0.0.jar
   code\target\lib\

4. 将 english-chinese-dictionary-1.0.0.jar 重命名为：

   english-chinese-dictionary.jar

5. 程序首次运行后，会在当前目录生成：

   app-data      保存历史和收藏
   runtime-home  保存 JavaFX 运行时缓存
