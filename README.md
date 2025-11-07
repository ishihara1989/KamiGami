# KamiGami

Minecraft 1.21.10用のNeoForge Modです。

## 必要な環境

- Java 21以上
- Minecraft 1.21.10
- NeoForge 21.10.43-beta以上

## ビルド方法

### Windows

```bash
gradlew.bat build
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
gradlew.bat runClient

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

### 式神システム
紙で作られた召喚可能なエンティティです。バニラの動物と似た動作をしますが、以下の特徴があります：

#### 共通仕様
- **低HP**: バニラの動物よりも脆弱（2～3ハート）
- **繁殖不可**: 繁殖できず、発情モードにも入りません
- **リサイクル可能**: 倒すと召喚アイテムをドロップ
- **資源取得**: バニラと同様に特定の資源を取得可能

#### 実装済みの式神
- **紙の牛** (Paper Cow) - ミルクが取れる（HP: 3ハート）
- **紙の鶏** (Paper Chicken) - 卵を産む（HP: 2ハート）
- **紙の羊** (Paper Sheep) - ハサミで羊毛が取れる（HP: 2.5ハート）

詳細は [gameplay-specification.md](docs/gameplay-specification.md) を参照してください。

## ライセンス

MIT License

## 作者

hydryhydra
