# Changelog

All notable changes to Gravely will be documented in this file.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) — [Semver](https://semver.org/)

## [Unreleased]

### Added
- 初版機能セット（Phase 1-5 全実装）
- 22 言語対応（ja/en + 機械翻訳ベース 20 言語）

### Notes
- Phase 7 (Architectury / Forge / Fabric 対応) と Phase 8 (1.20.1 backport) は将来予定

## [1.0.0] - YYYY-MM-DD (未公開)

### Added
- 墓ブロック (BaseEntityBlock) によるアイテム保管・回収
- DataComponent ベースの永続化
- 設置位置探索 (vertical/horizontal search + voidFallbackY)
- 水中対応 (SimpleWaterloggedBlock)
- アクセス制限モード (OWNER_ONLY / TIMED / NONE)
- XP 保管・復元
- Curios 連携（自動再装着）
- Sophisticated Backpacks 連携（DataComponent コピー）
- JourneyMap 連携（リアルタイム waypoint + 自動削除 + 標準 deathpoint 抑制 + 紫固定色）
- Soulbound エンチャント (datapack-enchantment, treasure 扱い)
- 共存ガード（HIGHEST priority + 競合検出ログ）
- 回収演出（WITCH パーティクル + 弔いの鐘）
- 死亡通知の墓座標クリック可能化（/tp コマンド SUGGEST）
- TIMED 保護モードの残り時間表示
- ブランドカラー紫の世界観統一

### Compatibility
- Minecraft 1.21.1
- NeoForge 21.1.227+
- Optional: Curios API, JourneyMap (1.21 NeoForge)
