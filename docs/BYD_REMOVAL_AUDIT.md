# BYD 依赖审计报告(移除前盘点)

> 目的:将 DiPlay(比亚迪定制版)适配为标致/雪铁龙(PSA)安卓 9 车机版,移除全部 BYD 专用代码。
> 基线:上游 DiPlay 0.2.10(fork 后未改动)。本报告记录每个 BYD 依赖点及其处理等级。

## 1. 全景盘点

### 1.1 `shared/.../hud/` 包(28 个文件)—— 纯 BYD,可整体删除

**外观层(编译入口)**
- `BydNavigationOutputs.kt` — 唯一被包外引用的门面(object),汇总全部输出
- `BydOutputSettings.kt` — SharedPreferences 设置门面(KEY_ENABLED 默认 true)
- `NavigationOutputWorker.kt` — 通用任务队列(仅被 hud 内部用)

**仪表盘桥接组(cluster bridge)**
- `BydClusterBridge.kt`(available = 装了 com.byd.amapservice)
- `BydClusterFrame.kt`、`BydClusterMapPause.kt`、`BydClusterNaviMode.kt`、`BydClusterSong.kt`
- `BydHudBridge.kt`(绑 SOME/IP:com.ts.car.someip.service)、`BydHudPayload.kt`、`BydHudProtocol.kt`

**独立 HUD 组(standalone,指纹锁定的 broadcast 输出)**
- `BydStandaloneHudOutput.kt`、`BydStandaloneNavigationBridge.kt`、`BydStandalonePackets.kt`、`BydStandaloneSession.kt`
- `BydStarterBridge.kt`、`BydFactoryNavigationOutput.kt`、`BydFactoryTurnCode.kt`

**ADB / 车辆数据组**
- `BydAdbAccess.kt`、`BydAdbShell.kt`(网路 ADB 客户端)、`BydBattery.kt`、`BydWheelSpeed.kt`、`BydParkedState.kt`

**⚠ 通用解析器(错放在 hud/,CarPlayGlance 依赖,不可直接删)**
- `BydHudRouteState.kt` — iAP2 路线引导解析器(0x5201/0x5202)
- `BydManeuverCodes.kt` — Apple RouteGuidanceManeuverType 分组
- `ClusterSongState`(定义在 `BydClusterSong.kt` 内)— iAP2 NowPlayingUpdate(0x5001)解析

### 1.2 `common/` 纯 BYD(可整体删除)
- `DiLink51ClusterLayout.kt`(supported() = Build.FINGERPRINT == BYD-AUTO/IVI...)
- `DiLink51ClusterMonitor.kt`(Usage Access 监控 cluster activity)
- `ClusterActivityState.kt`(com.byd.* 包名硬编码)
- `ClusterMapPresentation.kt`(仪表盘投影显示,com.byd.containerservice)

### 1.3 `common/` 通用文件带 BYD 钩子(需手术)
| 文件 | BYD 依赖 | 处理 |
|---|---|---|
| `CarPlayHostActivity.kt`(3971 行) | clusterPresentation/clusterMonitor/clusterLayers 字段 + ~15 个 cluster 方法(641-839 行区段);BydOutputSettings 调用(146-148, 2897, 3235, 3289-3290);HomeScreenMonitor | 删 cluster 块;BydOutputSettings 调用改常量/删除 |
| `DiPlayActivity.kt` | byd_navigation 设置区(319-470 行)+ ADB 状态卡(800-830);BydAdbAccess/BydNavigationOutputs/BydOutputSettings import | 删整个 section |
| `CarPlayVideo.kt` | `readParked()` → BydNavigationOutputs.parked | 改返回 null |
| `DiPlaySessionService.kt` | onTaskRemoved → BydNavigationOutputs.endNow() | 删调用 |
| `LocalizedOptions.kt` | BydClusterNaviMode.localizedLabel | 删扩展函数 |
| `AirPlayPersistence.kt` | `DEFAULT_OEM_LABEL = "BYD"`(88 行) | 改 "PSA" 或车机 OEM 名 |
| `HomeScreenMonitor.kt` | HOME_PACKAGES 含 com.byd.launchermap/naviauto/mycar | 换成 PSA 启动器包名(需实测) |

### 1.4 `shared/` 通用文件带 BYD 钩子
| 文件 | BYD 依赖 | 处理 |
|---|---|---|
| `orchestration/CarPlayController.kt` | BydNavigationOutputs 8 处(158-159 start/setClusterStreamControl、262/278/452/454 endNow/clear、532 onFrame) | 全部删除 |
| `glance/CarPlayGlance.kt` | import BydHudRouteState + ClusterSongState | 改 import 到迁移后的通用包 |
| `airplay/CarPlayMediaButton.kt` | BYD 方向盘 keycode 353/304/312 | 保留无害,或删 |
| 仅注释提及 | `adb/LocalAdb.kt`、`airplay/CarPlayClusterDisplay.kt`、`media/AndroidMediaSink.kt`、`network/CarHotspotStatus.kt`、`network/LocalOnlyHotspotManager.kt`、`network/P2pStartupRecovery.kt`、`transport/Iap2VehicleStatus.kt` | 可留可清,无代码影响 |

### 1.5 mobile debug + manifest + 资源
- `mobile/src/debug/java/.../hud/`:StandaloneHudDemoActivity、StandaloneHudPackets、StarterBridgeReceiver
- `mobile/src/debug/AndroidManifest.xml`:`BYDAUTO_INSTRUMENT_COMMON` 权限 + com.byd.clusterdebug query
- `shared/src/main/AndroidManifest.xml` queries:`com.byd.clusterdebug`、`com.ts.car.someip.service`、`com.byd.amapservice`
- 资源:`ic_dp_navigation.xml`;strings:byd_navigation、navi_mode_*、adb_*、battery/wheel_speed/video_while_parked/cluster_song 等(6 个 locale 各一份)

### 1.6 测试(需同步删/改)
- `common/src/test/`:ClusterActivityStateTest、ClusterMapPresentationTest、DiLink51ClusterLayoutTest
- `shared/src/test/hud/`:14 个测试(BydBattery、BydCluster*、BydHud*、BydStandalone*、BydWheelSpeed、NavigationOutputWorker 等)
- `shared/src/test/glance/CarPlayGlanceTest.kt` — 依赖 BydHudRouteState,迁移后保留

## 2. 内部依赖图(hud/ 内)

```
BydNavigationOutputs(门面) ──► BydBatteryStatus, BydClusterBridge, BydClusterMapPause,
   │                          BydClusterSong, BydHudBridge, BydHudRouteState,
   │                          BydOutputSettings, BydParkedState, BydStandaloneHudOutput,
   │                          BydStandaloneNavigationBridge, BydWheelSpeedSource, NavigationOutputWorker
BydOutputSettings ──► BydStandaloneHudOutput
BydHudRouteState ──► BydManeuverCodes
BydClusterBridge ──► BydClusterFrame, BydFactoryNavigationOutput, BydHudRouteState, BydOutputSettings
BydHudBridge ──► BydHudPayload, BydHudProtocol, BydHudRouteState, BydOutputSettings
BydStandaloneNavigationBridge ──► BydClusterFrame, BydHudRouteState, BydOutputSettings, BydStandaloneHudOutput
BydStandaloneSession ──► BydStandalonePackets ──► BydFactoryTurnCode
ADB 组:BydAdbAccess ──► BydBattery/BydClusterNaviMode;BydBattery/BydParkedState/BydWheelSpeed ──► BydAdbShell
```
包内无跨文件 import(同包直接引用),删除时整包移除即可,无内部遗留。

## 3. 运行期行为:非比亚迪车机上的现状(安全性评估)

关键防护——三重门,PSA 车机上几乎全部自动失效:

1. `BydStandaloneHudOutput.available()`:包名白名单 + **指纹锁** `BYD-AUTO/IVI/IVI:13/TP1A.220624.014/...` + com.byd.clusterdebug 签名 SHA-256 校验(efe3ca8a…)→ PSA 上 = false
2. `BydOutputSettings.available()` = standalone || com.byd.amapservice || com.ts.car.someip.service → PSA 上 = false → **设置页 byd_navigation 区块不显示**
3. `DiLink51ClusterLayout.supported()` = 指纹比对 → false

但 `CarPlayController` **每次会话仍无条件执行** `BydNavigationOutputs.start()`,导致:
- `BydHudBridge.initialize` → 无条件 `bindService` 到 com.ts.car.someip.service(失败静默)+ 启动 300ms 周期 daemon 线程,每 tick 重试 bind
- `BydClusterBridge.initialize` → available=false,早退,无副作用
- `BydClusterMapPause.initialize` → 每秒 tick,设置未启用即关闭 shell 早退(惰性)
- `BydClusterSong.attach` → 仅存 context

**结论:无崩溃风险,但每次 CarPlay 会话会常驻 2-3 个 daemon 线程 + 周期性 bind 尝试 + 日志噪音,不干净。** 另外 `onFrame` 管线每帧做 route.accept 解析(纯 CPU 小开销)。

## 4. 移除方案(分级)

### A. 整体删除(无包外依赖)
1. 整个 `shared/src/main/java/com/shilapi/xcertplay/hud/` 包(先迁移 C 组再删)
2. common 的 `DiLink51ClusterLayout/Monitor`、`ClusterActivityState`、`ClusterMapPresentation`
3. mobile/debug 的 3 个 hud 文件 + debug manifest
4. manifest queries 中 com.byd.clusterdebug / com.ts.car.someip.service / com.byd.amapservice
5. 资源:ic_dp_navigation + byd/adb/cluster/battery/wheel 相关 strings(6 locale)
6. 测试:common 3 个 + shared/hud 14 个

### B. 手术通用文件(先解除编译依赖)
1. `CarPlayController.kt`:删 8 处 BydNavigationOutputs 调用
2. `CarPlayHostActivity.kt`:删 cluster 字段/方法块 + BydOutputSettings 调用点
3. `DiPlayActivity.kt`:删 byd_navigation section + ADB 状态 UI
4. `CarPlayVideo.kt`:readParked 返回 null
5. `DiPlaySessionService.kt`:删 endNow 调用
6. `LocalizedOptions.kt`:删 BydClusterNaviMode 扩展

### C. 迁移保留(通用功能,错放的解析器)——**决策点**
- `BydHudRouteState` + `BydManeuverCodes` + `ClusterSongState` → 迁移到 `shared/.../glance/` 包
- `CarPlayGlance`(导航 widget 数据源)→ 改 import
- 理由:这些是纯 iAP2 协议解析(0x5201/0x5202/0x5001),与比亚迪硬件无关;`NavigationWidget`(标准 Android widget,任意启动器可用)依赖它
- **若放弃导航 widget 功能,则 C 组也可删除,删得更干净**

### D. 纯配置(最先做,见效快)
1. `AirPlayPersistence.DEFAULT_OEM_LABEL`:BYD → PSA(向 iPhone 声明的 OEM 名)
2. `HomeScreenMonitor.HOME_PACKAGES`:替换为 PSA 启动器包名(需在车机上实测)
3. `CarPlayMediaButton` BYD keycode:保留无害
4. 注释清理(1.4 表最后一行)

## 5. 建议执行顺序

1. **D**(改 OEM label)— 立即验证基础 CarPlay 在有线/无线下工作
2. **B**(解除编译依赖)— 每步跑一次 `:shared:compileDebugKotlin` 确认
3. **A**(物理删除)— 删除后全量编译 + 测试清理
4. **C 决策**(保留 widget vs 一并删除)— 迁移或删除
5. 全量构建:`.\gradlew.bat :mobile:assembleDebug :automotive:assembleDebug`

## 6. 第二清理阶段(fork 后续提交)

首轮移除(`22b8d72`)后,又按功能分组完成以下清理。本节的删除项与首轮无关,但同属"去掉 BYD 专用能力"。

### 6.1 组 2/3/6:已随首轮删净
风挡 HUD(`BydHud*`/`BydStandalone*`/`BydFactoryTurnCode`)、通用导航状态解析(`glance/CarPlayGlance`、`BydHudRouteState`、`BydManeuverCodes`)、调试设施(`mobile/src/debug` 的 HUD demo 与 StarterBridge)。导航 widget 与其布局/XML 也一并删除。

### 6.2 组 5 车辆数据回传:本次删除
- `shared/.../adb/{AdbKeys,AdbPacket,LocalAdb}.kt` 及测试
- `shared/.../transport/VehicleSpeedNmea.kt`(`$PASCD` 编码器 + `VehicleSpeedLocationProvider`)及测试
- `shared/.../transport/Iap2VehicleStatus.kt`(EV 声明、0xA100/0xA101/0xA102)及测试
- `shared/src/main/assets/byd-hud-icons/` 与 `shared/BYD_HUD_ICONS_NOTICE.md`;`docs/THIRD_PARTY_NOTICES.md` 相应条目
- `Iap2IdentificationConfig` 去掉 `vehicleStatusEnabled`/`chargingConnectors`/`vehicleSpeedEnabled`;`Iap2WiredControlClient`/`Iap2WirelessControlClient` 去掉 `vehicleStatusProvider` 形参、`withVehicleStatusFrom` 调用与 0xA100/0xA102 分支;`CarPlayController` 去掉同名构造形参。
- 结果:向 iPhone 不再声明电动车、电量、续航与轮速。CarPlay 其余能力不受影响。

### 6.3 组 4 中控地图卡:保留卡片,只删第三方桌面嵌入
- 删除 `common/.../MapEmbedService.kt`(+测试)、`common/.../HomeScreenMonitor.kt`、`AirPlayPersistence` 的 `launcher_map_sharing` 三个方法、`MapMirrors.launcherShowsMap`/`onChanged`/流活跃监听、`samples/home/`、`samples/maphost/` 及其 `settings.gradle.kts` include。
- manifest 去掉 `PACKAGE_USAGE_STATS`、`MapEmbedService` 声明与 HOME `<queries>`。
- 保留:`CenterMapOverlay.kt`(+测试)、`MapMirrors.CARD`/`STREAM_ASPECT`/`reapply()`、`CarPlayHostActivity.showCenterMap()` 接线、`AirPlayPersistence` 的 center-map 键。卡片仅需 SYSTEM_ALERT_WINDOW,不再受 Usage Access 限制。

### 6.4 驻车视频(VideoInCar):改为常驻允许
- 本 fork 无挡位数据源(android.car 属性在 Android 9 车机不可用,原有 BYD 挡位读取已随组 5 删除),`CarPlayVideo.readParked()` 恒返回 null,使该功能实际处于关闭状态。
- 现改为 `VideoInCar.allowed = true` 常量,删除 `VideoInCarGate` 轮询线程与 `readParked`/`onVideoAllowedChanged` 接口。
- **影响**:行车中也可播放视频,原"离开 P 挡即关闭播放器"的安全门不复存在;iPhone 侧会把车机视为支持车内视频。恢复门控需要另接挡位源。

### 6.5 组 1 仪表盘集群:暂不恢复
`ClusterMapPresentation`/`DiLink51ClusterLayout`/`DiLink51ClusterMonitor`/`ClusterActivityState` 已删除。`CarPlayClusterDisplay`、`AirPlayConfig.cluster`、stream 111 处理与 `AirPlayPersistence` 的 cluster 键保留为 PSA 恢复基础。要点:先实测 `dumpsys display` 确认液晶仪表是否暴露为 presentation display;BYD 版的找屏(层名)、指纹门、主题/可见性(Usage Access 监控)与 1920×720 几何都不可复用。`CarPlayHostActivity` 中把 `cluster = null` 改回布局的 `streamConfig()` 并复原 surface 交接(交接时不可先清旧 surface,否则解码器丢失参考帧、stream 111 会等 IDR)即接回链路。
