# クラフトレシピ一覧

このドキュメントはKamiGami Modで追加される全てのクラフトレシピをまとめています。

## 📋 目次
1. [基本素材](#基本素材)
2. [式神召喚アイテム](#式神召喚アイテム)
3. [その他](#その他)

---

## 基本素材

### 竹板 (Bamboo Planks)

**レシピタイプ:** Shaped Crafting

**材料:**
- 竹 (Bamboo) x4

**配置:**
```
BB
BB
```

**結果:**
- 竹板 x1

**ファイル:** `data/kamigami/recipe/bamboo_plank.json`

**説明:**
竹を2x2の正方形に配置してクラフトします。クラフトテーブル上で平行移動が可能です。

---

### 紙 (Paper)

**レシピタイプ:** Shaped Crafting

**材料:**
- 竹 (Bamboo) x3

**配置:**
```
BBB
```

**結果:**
- 紙 x3

**ファイル:** `data/kamigami/recipe/paper_from_bamboo.json`

**説明:**
竹を横一列に3つ並べてクラフトします。クラフトテーブル上で平行移動（上段、中段、下段のいずれでも可）が可能です。

---

## 式神召喚アイテム

### 紙の牛召喚 (Paper Cow Summon)

**レシピタイプ:** TBD (未実装)

**材料:**
- 紙 (Paper)
- 式神コア (Shikigami Core)
- その他材料

**配置:** TBD

**結果:**
- 紙の牛召喚 x1

**ファイル:** `data/kamigami/recipe/paper_cow_summon.json` (未作成)

**説明:**
紙の牛を召喚するためのアイテムを作成します。

---

### 紙の鶏召喚 (Paper Chicken Summon)

**レシピタイプ:** TBD (未実装)

**材料:**
- 紙 (Paper)
- 式神コア (Shikigami Core)
- その他材料

**配置:** TBD

**結果:**
- 紙の鶏召喚 x1

**ファイル:** `data/kamigami/recipe/paper_chicken_summon.json` (未作成)

**説明:**
紙の鶏を召喚するためのアイテムを作成します。

---

### 紙の羊召喚 (Paper Sheep Summon)

**レシピタイプ:** TBD (未実装)

**材料:**
- 紙 (Paper)
- 式神コア (Shikigami Core)
- その他材料

**配置:** TBD

**結果:**
- 紙の羊召喚 x1

**ファイル:** `data/kamigami/recipe/paper_sheep_summon.json` (未作成)

**説明:**
紙の羊を召喚するためのアイテムを作成します。

---

## その他

### 式神コア (Shikigami Core)

**レシピタイプ:** TBD (未実装)

**材料:** TBD

**配置:** TBD

**結果:**
- 式神コア x1

**ファイル:** `data/kamigami/recipe/shikigami_core.json` (未作成)

**説明:**
式神召喚アイテムのクラフトに必要な核となるアイテムです。

---

## レシピの追加方法

### 1. レシピファイルの作成

レシピJSONファイルは以下のディレクトリに配置します：

**重要:** Minecraft 1.21以降、ディレクトリ名は **`recipe`（単数形）** です！

```
src/main/resources/data/kamigami/recipe/
```

**注意:** 1.20以前は `recipes/`（複数形）でしたが、**1.21以降は `recipe/`（単数形）に変更されました**。間違ったディレクトリに配置するとレシピが読み込まれません。

### 2. ファイル命名規則

- **形式:** `snake_case` (小文字 + アンダースコア)
- **例:** `paper_cow_summon.json`, `bamboo_plank.json`

### 3. レシピタイプ

#### Shaped Crafting (形状ありクラフト)

配置が重要なレシピ。平行移動は許可されますが、回転や反転は不可。

**重要:** NeoForge 1.21.2以降、**バニラアイテムは文字列形式**で指定します！

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "pattern": [
    "AAA",
    " B ",
    " B "
  ],
  "key": {
    "A": "minecraft:paper",
    "B": "minecraft:stick"
  },
  "result": {
    "id": "kamigami:example_item",
    "count": 1
  }
}
```

**タグを使う場合:**
```json
{
  "type": "minecraft:crafting_shaped",
  "category": "building",
  "pattern": [
    "WW",
    "WW"
  ],
  "key": {
    "W": "#minecraft:planks"
  },
  "result": {
    "id": "kamigami:wood_item",
    "count": 4
  }
}
```

**フィールド説明:**
- `type`: レシピタイプ（`minecraft:crafting_shaped`）
- `category`: レシピブックでのカテゴリ
  - `building`: 建築ブロック
  - `redstone`: レッドストーン
  - `equipment`: 装備
  - `misc`: その他
- `pattern`: クラフト配置パターン（最大3x3）
- `key`: パターン内の文字と素材の対応
  - **バニラアイテム: 文字列 `"namespace:item"` で指定**
  - **タグ: 文字列 `"#namespace:tag"` で指定（先頭に`#`）**
- `result`: 完成品

#### Shapeless Crafting (形状なしクラフト)

配置が関係ないレシピ。

**重要:** NeoForge 1.21.2以降、**バニラアイテムは文字列形式**で指定します！

```json
{
  "type": "minecraft:crafting_shapeless",
  "category": "misc",
  "ingredients": [
    "minecraft:paper",
    "minecraft:stick",
    "#minecraft:planks"
  ],
  "result": {
    "id": "kamigami:example_item",
    "count": 1
  }
}
```

**フィールド説明:**
- `type`: レシピタイプ（`minecraft:crafting_shapeless`）
- `category`: レシピブックでのカテゴリ
- `ingredients`: 必要な材料のリスト（順不同）
  - **バニラアイテム: 文字列 `"namespace:item"` で指定**
  - **タグ: 文字列 `"#namespace:tag"` で指定（先頭に`#`）**
- `result`: 完成品

### 4. タグの使用

複数のアイテムを材料として受け入れる場合、タグを使用できます。

**重要:** NeoForge 1.21.2以降、タグは**文字列形式で`#`を先頭に付けて**指定します。

**✅ 正解:**
```json
{
  "key": {
    "W": "#minecraft:planks"
  }
}
```

**❌ 間違い（オブジェクト形式は使わない）:**
```json
{
  "key": {
    "W": {
      "tag": "minecraft:planks"
    }
  }
}
```

**使用例:**
```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["WW", "WW"],
  "key": {
    "W": "#minecraft:planks"
  },
  "result": {
    "id": "kamigami:planks_item",
    "count": 1
  }
}
```

これにより、オークの板、シラカバの板、マツの板など、`minecraft:planks`タグに含まれるすべての板材が材料として使用できます。

### 5. このドキュメントの更新

新しいレシピを追加した際は、必ずこのドキュメントも更新してください。

**追加する情報:**
- レシピタイプ
- 材料と数量
- 配置パターン（Shapedの場合）
- 結果アイテムと数量
- ファイルパス
- 簡単な説明

---

## レシピのテスト方法

### 1. コンパイル

```bash
./gradlew compileJava
```

エラーがないことを確認します。

### 2. ゲーム内テスト

```bash
./gradlew runClient
```

1. クリエイティブモードでワールドを作成
2. 必要な材料を取得
3. クラフトテーブルでレシピを試す
4. レシピブックに表示されるか確認

### 3. デバッグ

レシピが機能しない場合のチェックリスト：

- [ ] **ディレクトリ名が `recipe/`（単数形）か** ← 最重要！
- [ ] **バニラアイテム/タグを文字列形式で指定しているか** ← NeoForge 1.21.2+で必須！
  - アイテム: `"minecraft:stick"` （オブジェクト `{"item":"..."}` は使わない）
  - タグ: `"#minecraft:planks"` （オブジェクト `{"tag":"..."}` は使わない）
- [ ] ファイル名が正しいか（小文字、アンダースコア）
- [ ] JSONの構文が正しいか（カンマ、括弧）
- [ ] 材料のアイテムIDが正しいか
- [ ] 結果のアイテムIDが正しいか（1.21以降は `"id"` を使用）
- [ ] ファイルが正しいディレクトリにあるか
- [ ] ゲームを再起動したか
- [ ] ログファイル（`runs/client/logs/latest.log`）でエラーを確認
  - "Couldn't parse data file" エラーがないか確認
  - "Input does not contain a key [type]" または "[neoforge:ingredient_type]" エラーは、オブジェクト形式を使ってしまっている
- [ ] ログファイルで読み込まれたレシピ数を確認
  - 例: `Loaded 1463 recipes` (1461はバニラのみ、+2ならカスタムレシピが読み込まれている)

---

## 参考資料

- [Minecraft Wiki - Recipe](https://minecraft.wiki/w/Recipe)
- [Minecraft Wiki - Crafting](https://minecraft.wiki/w/Crafting)
- [NeoForge Documentation - Recipes](https://docs.neoforged.net/)

---

## 📝 更新履歴

### 2025-11-07
- ドキュメント作成
- 竹板レシピ追加
- 竹から紙へのレシピ追加
- **Minecraft 1.21のディレクトリ名変更に対応** (`recipes/` → `recipe/`)
- **NeoForge 1.21.2+のingredient記法変更に対応**
  - バニラアイテム: 文字列形式 `"namespace:item"` に統一
  - タグ: 文字列形式 `"#namespace:tag"` に統一
  - カスタムIngredient: オブジェクト形式（`neoforge:ingredient_type`必須）
  - すべてのレシピ例を最新の文字列形式に更新
- デバッグチェックリストを更新（文字列形式チェックを追加）

---

**最終更新日:** 2025-11-07
**更新者:** Claude Agent
