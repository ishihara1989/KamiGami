# KamiGami

**A Minecraft mod featuring mystical shrines and spiritual interactions, designed with skyblock gameplay in mind.**

---

## 概要

**KamiGami**は、神秘的な精霊との相互作用をテーマにしたMinecraft Modです。
祠を中心とした恩恵と災いのシステムにより、新しいゲームプレイ体験を提供します。

### このModの世界観

祠は基本的にプレイヤーに**恩恵**をもたらします。しかし、不用意に壊してしまうと**災い**が訪れます。
災いは時として新たな可能性を生み出し、それを乗り越えたときには**より大きな恩恵**が待っています。

このModはスカイブロックなどの限られた資源環境でのプレイも念頭に設計されています。

### 主な要素

- **祠システム**: 神秘的な力を宿す祠ブロック
  - お供え物や特殊効果によりプレイヤーに恩恵をもたらす（実装予定）
  - 不用意に破壊すると災いが訪れる
  - 災いを乗り越えることで新たな恩恵が得られる

- **式神**: 紙で作られた召喚可能な生物（紙の牛、紙の鶏、紙の羊）
  - 通常の動物よりも脆弱だが、倒しても召喚アイテムが戻ってくるためリサイクル可能
  - 繁殖はできないが、ミルク・卵・羊毛などの資源は通常通り取得可能
  - 限られた資源環境でも持続可能な資源取得手段を提供

Minecraft 1.21.10用のNeoForge Modです。

## 必要な環境

- Java 21以上
- Minecraft 1.21.10
- NeoForge 21.10.43-beta以上

## ビルド方法

### Windows

```bash
./gradlew.bat build
```

### Linux/Mac

```bash
./gradlew build
```

ビルドが完了すると、`build/libs/` フォルダに `.jar` ファイルが生成されます。

## 開発環境での起動方法

### クライアントの起動

```bash
# Windows
./gradlew.bat runClient

# Linux/Mac
./gradlew runClient
```

### サーバーの起動

```bash
# Windows
gradlew.bat runServer

# Linux/Mac
./gradlew runServer
```

## インストール方法

1. 上記の「ビルド方法」に従ってModをビルドします
2. 生成された `build/libs/kamigami-0.0.1.jar` をMinecraftの `mods` フォルダにコピーします
3. NeoForge 1.21.10がインストールされたMinecraftを起動します

## 開発ドキュメント

### 🤖 Claude Agent向け
このプロジェクトをClaude Agentで開発する場合は、まず **[CLAUDE.md](CLAUDE.md)** を読んでください。

### 📚 ドキュメント
プロジェクトに関する詳細なドキュメントは [`docs/`](docs/) フォルダにあります：

#### ゲームプレイ仕様
- **[gameplay-specification.md](docs/gameplay-specification.md)** - 式神システムの仕様とゲームプレイの詳細

#### 技術ドキュメント
- **[neoforge-gotchas.md](docs/neoforge-gotchas.md)** - NeoForge 1.21.10開発時の注意点とよくあるエラー
- **[entity-implementation-guide.md](docs/entity-implementation-guide.md)** - カスタムエンティティの実装手順
- **[project-structure.md](docs/project-structure.md)** - プロジェクト構造とファイル命名規則

## 実装済み機能

このModの詳細なゲームプレイ仕様については、[gameplay-specification.md](docs/gameplay-specification.md) を参照してください。

### 主な実装内容

#### 祠システム
- アイテムを保管できる神秘的な祠ブロック
- シルクタッチなしで破壊すると災い（祟り）が発生
- 御神体の種類に応じた異なる災いと恩恵
- スカイブロック向けの資源変換システム

#### 祟りモンスター
- **祟りスライム**: 黒いスライム型、サイズに応じて分裂
- **豊穣の祟り**: 木型の遠距離攻撃モンスター、斧に弱い

#### 式神（紙の召喚生物）
- **紙の牛**: ミルク取得可能
- **紙の鶏**: 卵を産む
- **紙の羊**: 羊毛刈り取り可能
- リサイクル可能な召喚システム（倒すと召喚アイテムが戻る）

## ライセンス

MIT License

## 作者

hydryhydra
