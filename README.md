# project-0017 生存服分流系统

## 项目说明

- 名称：生存服分流系统（和平/战斗 × 电脑/手机）
- 编号：0017
- 开始日期：2026-08-17
- 状态：v1.1.0 已部署并热载生效，模组审计待实战验证

## 内容

- `paper/`：Paper 1.21.11 服务端插件 `SurvivalSplit`，部署到 25568 生存服。
- 生存服上的 KaelorvynTitle 会读取 `ProfileStore`，在玩家称号尾缀显示
  红色 `[战斗]`、绿色 `[和平]` 或白色 `[无分流]`（legacy/未装模组）。
- 每次进服都会强制弹出身份 GUI（和平/战斗 + 电脑/手机），关掉会自动重弹，
  选择后立即更新档案；平台由模组动态识别，今天电脑明天手机可自动切换。
  - 选择完成后会立即通知 KaelorvynTitle 刷新阵营尾缀，不等 5 秒周期刷新。
  - 已装客户端模组的玩家只显示“和平/战斗”一行，电脑/手机由模组
    自动识别并锁定；未装模组的玩家才显示电脑/手机选择行。
  - 握手超时未装模组的玩家自动按“旧版电脑战斗玩家”处理：不弹窗、
    无提示；打不了和平玩家，但和平玩家可以打他，与战斗/legacy 正常互伤。
  - 和平玩家与战斗玩家双向 PvP 隔离，和平对和平也禁止；TNT/末影水晶/重生锚按归属解析。
  - 手机战斗玩家伸手距离 +1（方块 5.5 / 实体 4.0），其余保持默认。
  - 档案存 `plugins/SurvivalSplit/profiles.yml`。
- `fabric/`：客户端视觉模组 `SurvivalSplit-Client`。
  - 服务端通过 `survivalsplit:sync` 同步阵营，模组对跨阵营玩家身体渲染 70% 透明度，名字不透明。
  - 模组每次进服通过 `survivalsplit:plat` 上报检测到的设备类型（PC/MOBILE）。
  - Mod Menu 提供“虚化不透明度”滑动条（0-100）：100 完全显示，
    0 身体完全隐藏只剩名字，默认 35。
  - 模组每次进服通过 `survivalsplit:modlist` 上报已加载模组列表（id/name/version/description）。
  - 服务端保存每玩家模组指纹；指纹变化时才联网审计，省 AI 资源。
  - 未装模组客户端由服务端通过 PacketEvents 单独下发跨阵营目标的发光描边。
- `outputs/`：构建产物。

## 客户端模组审计（v1.1.0）

- 流程：进服上报模组列表 → 指纹比对 → 新增/变化模组用 Tavily 真搜索 →
  Agnes AI 判定 `safe / cheat / suspicious / unknown` →
  `cheat` 提交给 KaelorvynGuard 同一踢出界面/路径，但不计警告、不封号，
  只踢出并提示移除作弊模组；
  `suspicious / unknown` 默认告警。
- 内置作弊模组名单命中后不联网直接封号，名单可在 `modaudit.cheat-mods` 配置追加。
- 已解包确认的两个 W 系外挂：`wurst`（Wurst Client）、`wurstpenguin`
  （WurstB+ Plus），已直接写入黑名单，命中后不再联网搜索，直接提交
  KaelorvynGuard 统一处置。
- 黑名单优先级高于模组指纹去重：带已知作弊模组每次进服都会直接拦截，
  管理员权限（`kg.bypass` / `survivalsplit.modaudit.bypass`）不再豁免模组审计。
- 历史数据存 `plugins/SurvivalSplit/mod-audit.yml`。
- Tavily 密钥已写入线上 `plugins/SurvivalSplit/config.yml`；AI Key 留空时自动
  读取 `AGNES_API_KEY` 或 KaelorvynGuard 的 `ai.api-key`。
- 豁免权限：`survivalsplit.modaudit.bypass` 或 `kg.bypass`。
- 修改 `modaudit.*` 后执行 `/split reload` 立即生效。

## 构建

```powershell
cd paper
.\gradlew.bat assemble runTests
cd ..\fabric
.\gradlew.bat build
```

## 部署

- 插件：复制 `outputs\SurvivalSplit-1.1.0.jar` 到
  `D:\MC\server\[25568]lifesteal生存\plugins`，再 `plugman load SurvivalSplit` 或重启生存服。
- 客户端模组：复制 `outputs\SurvivalSplit-Client-1.1.0.jar` 到
  `D:\MC\.minecraft\versions\1.21.11-Fabric 0.19.3\mods`。

## 指令与权限

- `/split`：打开身份选择界面。
- `/split check`：查看当前身份。
- `/split reload`：重载配置（需要 `survivalsplit.admin`，默认 OP）。

## 配置要点

- `gui.always-show`：默认 `true`，每次进服强制重新选择；改 `false` 只对未选择玩家弹窗。
- `gui.reopen-delay-ticks`：关闭选择界面后自动重弹的延迟，默认 1 tick。
- `gui.mod-detect-wait-ticks`：等待客户端模组握手的 tick 数，默认 20；
  超时未握手按 legacy 电脑战斗玩家处理。
- `reach.*`：各身份交互距离。
- `visual.fallback-glow`：未装客户端模组时的发光描边兜底开关。
