### Demo运行说明
Demo地址： **[LPR](https://github.com/AleynP/LPR)**
打开项目肯定会报编译错误，要做以下修改：
1. 用AS打开项目
2. 设置项目NDK为 NDK-r14b
3. 先修改 CMakeLists.txt 文件， 把第19行修改成你本地的 OpenCV SDK 的对应路径。
4. 跟据自己的开发平台，设置app下的build.gradle  第 23 行代码 是否要注释。
完成以上步骤后再运行项目，就没有问题了。
## 更新（2020-6-2）：
1，更改了取景框适配问题
2，更改成AndroidX，使用了Google 的 CamreaX 来做相机预览
3，分离出ocr Module ，为了方便其他项目导入识别功能
