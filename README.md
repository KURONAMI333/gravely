# Gravely

> A clean, lightweight grave mod with broad mod compatibility.

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## Why Gravely?

「お墓 MOD はもうたくさんある」のはその通りで、Gravely は **3つだけ尖ってる**:

1. **シンプル** — Mixin ゼロ、ブロック1個、JSON駆動。1.21 NeoForge の流儀に沿った最小構成
2. **互換性丁寧** — Curios slot / Sophisticated Backpacks / JourneyMap (リアルタイム waypoint + 自動削除 + 標準 deathpoint 抑制) を一通り押さえる
3. **Soulbound 尊重** — 自前の `gravely:soulbound` エンチャントに加え、他 MOD の同種エンチャも将来的に統合（タグ駆動）

「フル機能・装飾・ボス」が欲しいなら [Corail Tombstone](https://www.curseforge.com/minecraft/mc-mods/corail-tombstone) を、サーバーサイドのみが良いなら [Universal Graves](https://modrinth.com/mod/universal-graves) を、**「軽くてシンプル + よく出来た互換性」が欲しいなら Gravely を**。

---

## Features

- 🪦 **死亡時に墓ブロックを自動生成** — インベントリ + Curios slot + 経験値を保管
- 🔓 **所有者のみ開けるアクセス制限** — `OWNER_ONLY` / `TIMED` (時間経過で誰でも) / `NONE` の3モード
- 💧 **水中対応** — Waterlogged ブロックとして水を保持、流れない・消えない
- 🌋 **マグマ・空中・狭い穴** — 設置位置探索が安全な場所を自動で探す（縦/横/虚空フォールバック）
- ⚔️ **Curios 連携** — Accessory slot のアイテムも保管・回収。装着可能なら自動で再装着
- 🎒 **Sophisticated Backpacks 連携** — バックパックが中身ごとそのまま墓に保管
- 🗺️ **JourneyMap 連携** — 死亡地点を waypoint として自動登録、回収時に自動削除（標準 deathpoint は抑制して重複なし）
- 👻 **Soulbound エンチャント** — 装備に付与すると死亡しても墓に入らずインベントリに残る（司書取引・戦利品で稀に登場）
- ✨ **回収演出** — 紫の魔法粒 + 軽快なサウンド
- 🛡️ **共存ガード** — 他お墓 MOD と一緒に入れても事故らない優先処理

---

## Installation

1. [NeoForge 21.1.227+](https://neoforged.net) を導入
2. (任意) [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios) を導入（アクセサリ MOD 使う場合）
3. `gravely-x.y.z.jar` を `mods/` フォルダに放り込む

---

## Configuration

`config/gravely-common.toml` で設定可能。NeoForge の Mod Settings GUI からも触れる。

| カテゴリ | キー | 既定 | 説明 |
|---|---|---|---|
| `placement` | `verticalSearch` | 16 | 墓設置位置の上下探索範囲 (ブロック) |
| `placement` | `horizontalSearch` | 4 | XZ方向のフォールバック探索半径 |
| `placement` | `voidFallbackY` | 64 | 虚空死亡時の代替Y座標 |
| `placement` | `allowFluid` | true | 水中への墓設置を許可 |
| `protection` | `mode` | OWNER_ONLY | 保護モード (OWNER_ONLY / TIMED / NONE) |
| `protection` | `timedDurationSec` | 3600 | TIMED モード時の保護時間 (秒) |
| `protection` | `opCanBypass` | true | OP は常に開ける |
| `notification` | `graveCreated` | true | 墓生成時にチャット通知 |
| `feature` | `enabled` | true | MOD 全体の有効/無効 |
| `feature` | `storeXp` | true | 経験値も墓に保管 |
| `feature` | `removeBlockOnRecover` | true | 回収時に墓ブロックを撤去 |

---

## Compatibility

| MOD | サポート状況 | 動作 |
|---|---|---|
| **Curios API** | optional (側) | アクセサリ slot のアイテムを保管・回収・自動再装着 |
| **JourneyMap** | optional (CLIENT) | 墓位置を自動 waypoint 化、紫色固定、回収で自動削除、標準 deathpoint 抑制 |
| **Sophisticated Backpacks** | 自動対応 | DataComponent 越しにバックパックごと保管 |
| **Xaero's Minimap/Worldmap** | 標準 deathpoint 利用 | 別途連携不要 (Xaero 標準の "Latest Death" 機能で位置確認可) |
| **他のお墓 MOD** | 共存ガード付き | Gravely が `EventPriority.HIGHEST` で先に drops を引き取る。起動時にログ警告 |

---

## Soulbound エンチャント

Gravely は独自の `gravely:soulbound` エンチャントを提供:

- **入手**: 司書 (librarian) との取引、ダンジョン戦利品で稀に登場（エンチャ台では出ない）
- **コマンド入手**: `/give @s diamond_sword[enchantments={"gravely:soulbound":1}]`
- **挙動**: 装備に付与すると死亡時に墓に入らず、リスポーン時にインベントリへ自動復元
- **対象**: バニラの `#minecraft:enchantable/durability` (剣・防具・ツール・釣り竿・エリトラ等)
- **排他**: `binding_curse` / `vanishing_curse` と同時付与不可
- **金床コスト**: 4
- **レア度**: weight 2 (Mending と同等)

---

## FAQ

**Q. 既存ワールドに途中から入れて壊れない？**  
A. はい、安全です。新規にロード時から有効化されます。

**Q. 他のお墓 MOD と併用できる？**  
A. 可能ですが、Gravely が優先されます (`EventPriority.HIGHEST` + `event.setCanceled`)。起動時にログで警告を出します。重複する機能は片方を無効化する方が安定します。

**Q. modpack に組み込んでいい？**  
A. もちろん。MIT ライセンスなので modpack 利用 OK、許可・通知不要です。

**Q. JourneyMap 標準の死亡 waypoint と被るのでは？**  
A. 被りません。Gravely は JM API の `DeathWaypointEvent` をキャンセルして標準機能を抑制し、Gravely 専用の紫 waypoint だけ残します。

**Q. シングルプレイとマルチプレイ両方で動く？**  
A. 両方対応。マルチサーバでは `Gravely` をサーバ・全クライアントに配布してください (JourneyMap 連携はクライアント側のみ要)。

**Q. 1.20 や Forge / Fabric は対応する？**  
A. 現在は NeoForge 1.21.1 のみ。将来計画で対応予定 (`_docs/ROADMAP.md` 参照)。

---

## Bug Reports / Feature Requests

GitHub Issues に投げてください: [Issues](https://github.com/KURONAMI333/gravely/issues)

---

## Translations

Gravely は 22 言語に対応:

`en_us` / `ja_jp` / `zh_cn` / `zh_tw` / `ko_kr` / `ru_ru` / `pt_br` / `es_es` / `de_de` / `fr_fr` / `it_it` / `pl_pl` / `uk_ua` / `cs_cz` / `tr_tr` / `nl_nl` / `sv_se` / `vi_vn` / `th_th` / `id_id` / `fi_fi` / `hu_hu`

> ⚠️ ja/en 以外は **機械翻訳ベース**です。文法や自然さに違和感があるかもしれません。Native speaker による校正の Pull Request を歓迎します。

## License

[MIT License](LICENSE) — modpack 利用、改変、再配布 OK。クレジット不要ですが歓迎。

---

## Credits

- Author: KURONAMI333
- 参考: [Corail Tombstone](https://www.curseforge.com/minecraft/mc-mods/corail-tombstone), [gravestone (henkelmax)](https://modrinth.com/mod/gravestone-mod), [YIGD](https://modrinth.com/mod/yigd), [Universal Graves](https://modrinth.com/mod/universal-graves), [awildhooman/Soulbound Mod](https://modrinth.com/mod/awildhooman-soulbound-enchantment)
