# 動的テクスチャ生成システム

このドキュメントでは、KamiGamiで実装されている動的テクスチャ生成システムについて説明します。

## 概要

KamiGamiでは、バニラテクスチャを実行時に加工して使用することで、カスタムテクスチャファイルの管理を削減しています。

### 実装済み機能

1. **単純なテクスチャ処理** - 彩度・輝度調整
2. **テクスチャアトラス生成** - 複数テクスチャの結合と色相回転

## TextureProcessor クラス

**場所**: `src/main/java/com/hydryhydra/kamigami/client/util/TextureProcessor.java`

### 主要メソッド

#### 1. processTexture() - 単純な処理

```java
public static ResourceLocation processTexture(
    ResourceLocation vanillaTexture,
    float saturation,      // 0.0 = グレースケール, 1.0 = 元の彩度
    int brightness,        // -255 ~ 255 の範囲で調整
    String cacheKey        // キャッシュ用の一意な識別子
)
```

**使用例**: 祟りスライム（TatariSlimeEntity）

```java
// バニラスライムテクスチャを彩度0（グレースケール）、輝度-50で処理
ResourceLocation processedTexture = TextureProcessor.processTexture(
    ResourceLocation.withDefaultNamespace("textures/entity/slime/slime.png"),
    0.0F,  // 完全にグレースケール化
    -50,   // 暗くする
    "tatari_slime_processed"
);
```

**処理内容**:
- バニラスライムテクスチャを読み込み
- 各ピクセルをグレースケール化（彩度0）
- 輝度を-50して暗くする
- 処理済みテクスチャをDynamicTextureとして登録
- キャッシュに保存（同じcacheKeyでの再処理を防ぐ）

---

#### 2. createTatariTreeAtlas() - アトラス生成

```java
public static ResourceLocation createTatariTreeAtlas(String cacheKey)
```

**使用例**: 豊穣の祟り（TatariFertilityEntity）

```java
ResourceLocation textureAtlas = TextureProcessor.createTatariTreeAtlas("tatari_tree_atlas");
```

**処理内容**:
1. バニラテクスチャを読み込み:
   - `oak_log.png` - 樫の丸太（側面）
   - `oak_log_top.png` - 樫の丸太（上面）
   - `carved_pumpkin.png` - くり抜きかぼちゃ（顔）
   - `pumpkin_side.png` - かぼちゃ（側面）
   - `pumpkin_top.png` - かぼちゃ（上面）
   - `oak_leaves.png` - 樫の葉

2. 各テクスチャに色相回転と輝度調整を適用:
   - **色相回転**: -90° (オレンジ系→緑系)
   - **輝度調整**: -50 (暗くする)

3. 256x256のアトラスに配置:
   - モデルのUV座標に合わせて配置
   - 必要に応じてリサイズ（nearest-neighbor）

4. DynamicTextureとして登録してキャッシュ

---

### 色相回転の実装

#### HSV色空間での回転

RGB値を直接操作すると不自然な色になるため、HSV色空間を使用して色相を回転します。

**処理フロー**:
```
RGB → HSV → 色相回転 → RGB
```

**色相回転の例**:
- オレンジ（約30°）- 90° = **-60°** = **300°** (赤紫)
- 赤（0°）- 90° = **-90°** = **270°** (マゼンタ)

実際には360°でラップアラウンドするため:
```java
hsv[0] = (hsv[0] - 90.0f + 360.0f) % 360.0f;
```

**色相環での位置**:
- 0° = 赤
- 60° = 黄
- 120° = 緑
- 180° = シアン
- 240° = 青
- 300° = マゼンタ

**調整方法**:
色合いを変更したい場合は、回転角度を調整します：

```java
// 緑にしたい場合: -90°
hsv[0] = (hsv[0] - 90.0f + 360.0f) % 360.0f;

// 青にしたい場合: +180° または -180°
hsv[0] = (hsv[0] + 180.0f) % 360.0f;

// シアンにしたい場合: +120°
hsv[0] = (hsv[0] + 120.0f) % 360.0f;
```

---

### デバッグ機能

#### デバッグ座標グリッド

テクスチャアトラスのUVマッピングを確認するため、デバッグ用の座標表示機能があります。

**有効化方法**:

`TextureProcessor.java` の以下の行のコメントを外します：

```java
// DEBUG: Uncomment to draw coordinate grid for debugging
// drawDebugGrid(atlas);
```

↓

```java
// DEBUG: Uncomment to draw coordinate grid for debugging
drawDebugGrid(atlas);
```

**機能**:
- 各16x16タイルに座標を16進数で表示
- 左上が (0,0)、右下が (F,F)
- 座標は白色の3x5ピクセルフォントで描画

**使用シーン**:
- UV座標のマッピングが正しいか確認したい
- テクスチャが期待通りの位置に配置されているか検証

---

## テクスチャアトラスのUVマッピング

### Tatari Treeのアトラスレイアウト (256x256)

#### 1. 幹（Trunk）- texOffs(0, 0)

| 座標 (hex) | ピクセル位置 | テクスチャ内容 |
|-----------|------------|--------------|
| (0,0)-(0,5) | (0,0)-(0,80) | log side (前面) |
| (1,0) | (16,0) | log TOP |
| (1,1)-(1,5) | (16,16)-(16,80) | log side |
| (2,0) | (32,0) | log TOP |
| (2,1)-(2,5) | (32,16)-(32,80) | log side |
| (3,0)-(3,5) | (48,0)-(48,80) | log side (左面) |
| (0,6)-(3,6) | (0,96)-(48,96) | log side |

#### 2. 頭（Head/Jack-o-Lantern）- texOffs(0, 112)

| 座標 (hex) | ピクセル位置 | テクスチャ内容 |
|-----------|------------|--------------|
| (0,7) | (0,112) | pumpkin side |
| (1,7) | (16,112) | pumpkin TOP |
| (2,7) | (32,112) | pumpkin TOP |
| (3,7) | (48,112) | pumpkin side |
| (0,8) | (0,128) | pumpkin top |
| (1,8) | (16,128) | **face** (carved_pumpkin) |
| (2,8) | (32,128) | pumpkin side |

#### 3. 枝（Branches）- texOffs(64, 0)

| 座標 (hex) | ピクセル位置 | テクスチャ内容 | サイズ |
|-----------|------------|--------------|--------|
| (4,0)-(5,0) | (64,0)-(95,0) | log side (前面) | 32x8 |
| (6,0) | (96,0) | log side (右面) | 8x8 |
| (7,0) | (112,0) | log side | 16x8 |
| (8,0)-(9,0) | (128,0)-(159,0) | log side (後面) | 32x8 |
| (A,0) | (160,0) | log side (左面) | 8x8 |
| (4,0)-(5,0) | (64,8)-(95,8) | log TOP (上面) | 32x8 |
| (6,0)-(7,0) | (96,8)-(127,8) | log TOP (下面) | 32x8 |

#### 4. 葉（Leaves）- texOffs(64, 16)

| 座標 (hex) | ピクセル位置 | テクスチャ内容 |
|-----------|------------|--------------|
| (4,1)-(7,1) | (64,16)-(127,16) | leaves (4面分) |
| (4,2)-(7,2) | (64,32)-(127,32) | leaves (上下+2面) |

**重要**: モデルの `texOffs()` 呼び出しとアトラスの座標が対応している必要があります。

---

## キャッシュ機構

### キャッシュの仕組み

```java
private static final Map<String, ResourceLocation> processedTextureCache = new HashMap<>();
```

- `cacheKey` をキーとして、生成済みテクスチャの `ResourceLocation` を保存
- 同じ `cacheKey` で再度呼ばれた場合、処理をスキップしてキャッシュから返す
- メモリ効率とパフォーマンスの向上

### キャッシュのクリア

リソースパックのリロード時などに呼び出す：

```java
TextureProcessor.clearCache();
```

---

## NativeImage フォーマット

### ABGR ピクセル形式

Minecraftの `NativeImage` は **ABGR** 形式でピクセルを格納します：

```
32bit = [Alpha 8bit][Blue 8bit][Green 8bit][Red 8bit]
```

**ピクセル操作**:

```java
int pixel = image.getPixel(x, y);

// 展開
int a = (pixel >> 24) & 0xFF;  // Alpha
int b = (pixel >> 16) & 0xFF;  // Blue
int g = (pixel >> 8) & 0xFF;   // Green
int r = pixel & 0xFF;          // Red

// 再構築
int newPixel = (a << 24) | (b << 16) | (g << 8) | r;
image.setPixel(x, y, newPixel);
```

---

## 実装時の注意点

### 1. RGB ↔ HSV 変換の精度

浮動小数点演算を使用しているため、わずかな誤差が発生する可能性があります。
ピクセルアート的な質感を維持するため、`nearest-neighbor` リサンプリングを使用しています。

### 2. パフォーマンス

- テクスチャ処理は初回起動時（レンダラー初期化時）のみ実行
- キャッシュにより2回目以降は即座に返される
- 大量のピクセル処理が必要な場合、起動時間に影響する可能性あり

### 3. リソースのクリーンアップ

```java
// 使用後は必ずcloseする
originalImage.close();
processedImage.close();
```

`NativeImage` はネイティブメモリを使用するため、適切にクローズしないとメモリリークの原因になります。

---

## 今後の拡張

### 追加可能な処理

1. **彩度調整** - 現在はグレースケール化のみだが、部分的な彩度調整も可能
2. **コントラスト調整** - 明暗の差を強調
3. **ガンマ補正** - より自然な明るさ調整
4. **テクスチャブレンド** - 複数テクスチャの合成
5. **ノイズ追加** - より有機的な質感

### 新しいエンティティへの適用

同様の手法で、他のバニラテクスチャを加工してカスタムエンティティに使用可能：

```java
// 例: 青い牛
NativeImage blueCow = processTextureImage(
    loadTexture(resourceManager,
        ResourceLocation.withDefaultNamespace("textures/entity/cow/cow.png")),
    true,  // 色相回転: 赤→青 (+180°)
    0      // 輝度変更なし
);
```

---

## 参考リンク

- [NeoForge Documentation - Client-side rendering](https://docs.neoforged.net/)
- [Minecraft Wiki - Textures](https://minecraft.wiki/w/Texture)
- [HSV色空間 - Wikipedia](https://ja.wikipedia.org/wiki/HSV%E8%89%B2%E7%A9%BA%E9%96%93)

---

**最終更新日**: 2025-11-13
**更新者**: Claude Agent
