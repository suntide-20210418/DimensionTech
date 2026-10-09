/**
 * DimensionTech 对外稳定 API（供其它模组做可选联动）。
 *
 * <p>包内只提供无状态门面与纯数据记录，<b>不新增算法</b>：所有计算都转调模组内部既有实现 （{@code StructureValueCalculator} / {@code
 * StructureLootAnalyzer} / {@code ChestMarkerItem}）。
 *
 * <p>兼容约定：本包的公开类型与签名视为对外契约，改动需保持源兼容（宁可新增重载，不要改既有签名）。 调用方需自行判断 mod 是否加载（{@code
 * ModList.get().isLoaded("dimension_tech")}）并在缺失时降级， 禁止在未安装 DimensionTech 的环境下触发本包类的加载。
 */
package com.suntide_20210418.dimensiontech.api;
