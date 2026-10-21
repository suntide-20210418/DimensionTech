# RED WEDGE 视觉与连接纹理更新

目标环境：Minecraft 1.20.1、Forge 47.4.10、Java 17。

## 效果与范围

- 统一机壳、物品、工具与 GUI 的暖白、石墨和朱红配色，采用苏联构成主义与先锋艺术方向的几何图形，保持像素边界。
- 六阶采掘器使用各自独立的原有模型与图案，成型状态保留等级区别。
- 机壳、结构块和玻璃使用原生 47 态连接纹理；合并内部边框与角标，保留外边界和凹角；剔除相邻玻璃接触面。
- 多方块成型后使用连续法兰、顶面与立柱图案；散装升级保留功能标记，成型立柱使用整体大楔形。
- 面配置显示关闭、输入、输出、双向四态；AE 与自动拉取开关具有凸起/下陷和中央圆形差异。

## 实现

连接模型通过 Forge 的烘焙模型与 ModelData 路径参与区块静态网格。邻接只在 getModelData 中读取，getQuads 返回不可变缓存。未增加部件方块实体或 BER。

成型外观位置通过 assembly_slot 方块状态保存与同步；主机 assembled 状态用于结构生命周期判断。观察与刷新不强制加载区块。网络协议为 11-redwedge，需要客户端和服务端使用匹配构建。

src/main/resources/assets/dimension_tech 包含图片、模型与 GUI；art-source/svg 提供精确的可编辑界面和连接材质源文件。最终分发包含 44 份散装/备用模型、18 种物品图标、六张 GUI 和 212 份原生材质辅助模型。三个简化主机草稿材质未用于替换实际主机。

## 接入

本目录是相对上游归档的覆盖改动，需放入匹配的项目根目录。基线信息记录于 BASELINE.json；归档全部文件已与上游提交 3577594bdf04c5948e6318b8b2653f06c3550e92 核对一致，并保留归档 SHA-256。维护者可在审查后移植到当前分支，并调整发布版本号。

资源包只能覆盖图片和 JSON。连接、成型与面配置行为需要一并合入 Java 修改。新增的 empty.snbt 为测试场景，不参与正常游戏逻辑。

构建命令：

```powershell
.\gradlew.bat test build -PvanillaLootRuntime=true
```

可选客户端模型验收命令：

```powershell
.\gradlew.bat runClient -PvanillaLootRuntime=true -PredwedgeModelValidation
```

该模型验收模式在完成加载和检查后自动退出。普通客户端不会启用此模式。最终版本未重新启动游戏集成测试，当前证据和待验收项目见 red-wedge-review/VALIDATION.md。

## 来源与许可

上游项目：https://github.com/suntide-20210418/DimensionTech 。保留原项目版权声明、作者字段及 GPL-3.0 许可证。参考 GregTech Modern、AdvancedAE 等项目的连接与成型组织方式，未复制这些项目的贴图。
