"创建函数的控制流图（CFG），以图片格式显示。仅在 Ubuntu 上测试过。" +
"\n\n使用方法：使用 Jadx-GUI 打开一个反编译后的 Java 代码界面，鼠标放在一个函数名上右键 -> 查看控制流图。" +
"\n\n选项：生成控制流图的类型：普通，RAW（包含原始 Instructions）， REGION（显示 Region 的划分）。" +
"在 Jadx 的首选项中可以更改默认值。" +
"\n\n需要安装：graphviz（用到了 'dot' 命令将 dot 格式转为 png）。" +
"\n\n原理：使用 Jadx 的 DotGraphVisitor.dump() / dumpRaw() / dumpRegion() 创建 .dot 文件，" +
"然后使用 'dot -Tpng' 命令将其转为 png 图片并显示。"

"Generate method's CFG and display it as image. Tested on ubuntu." +
"\n\nUsage: Open a decompiled Java code tab and right click on a method's name -> view CFG." +
"\n\nOptions: Dump CFG as GENERAL, RAW (raw instructions) or REGION (region rectangles). " +
"You can change the default dump type in the Jadx preferences dialog." +
"\n\nPrerequisite: graphviz installed. (command 'dot' is used to convert .dot format to png)" +
"\n\nHow it works: Create .dot file using Jadx's DotGraphVisitor.dump()/dumpRaw()/dumpRegion(). " +
"Then convert it to png using 'dot -Tpng' command."
