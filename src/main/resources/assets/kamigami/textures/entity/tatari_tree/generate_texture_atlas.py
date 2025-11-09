#!/usr/bin/env python3
"""
Tatari Tree テクスチャアトラス生成スクリプト

TATARI_TREE_TEXTURE_GUIDE.md に基づいて、256x256ピクセルの
テクスチャアトラスを生成します。
"""

from PIL import Image
import os


def create_texture_atlas():
    """256x256のテクスチャアトラスを作成する"""

    # 256x256の透明な画像を作成
    atlas = Image.new('RGBA', (256, 256), (0, 0, 0, 0))

    # 素材ファイルを読み込む
    try:
        log_side = Image.open('tatari_tree_log.png').convert('RGBA')
        log_top = Image.open('tatari_tree_log_top.png').convert('RGBA')
        head_face = Image.open('tatari_tree_head_face.png').convert('RGBA')
        head_side = Image.open(
            'tatarI_tree_head_side.png').convert('RGBA')  # 大文字I
        head_top = Image.open('tatari_tree_head_top.png').convert('RGBA')
        leaves = Image.open('tatari_tree_leaves.png').convert('RGBA')
    except FileNotFoundError as e:
        print(f"エラー: ファイルが見つかりません - {e}")
        return None

    print("素材ファイルを読み込みました")

    # 1. 幹 (Trunk) - texOffs(0, 0)
    # サイズ: 16x96x16ピクセルのボックス
    print("幹のテクスチャを配置中...")

    # 側面テクスチャを16x16にリサイズ
    log_side_16 = log_side.resize((16, 16), Image.Resampling.NEAREST)

    # 幹の側面（16x96を4面）
    # 前面 (0, 0)
    for i in range(6):  # 96 / 16 = 6
        atlas.paste(log_side_16, (0, i * 16))

    # 右側面 (16, 0)
    for i in range(6):
        atlas.paste(log_side_16, (16, i * 16))

    # 後面 (32, 0)
    for i in range(6):
        atlas.paste(log_side_16, (32, i * 16))

    # 左側面 (48, 0)
    for i in range(6):
        atlas.paste(log_side_16, (48, i * 16))

    # 上面 (0, 96)
    log_top_16 = log_top.resize((16, 16), Image.Resampling.NEAREST)
    atlas.paste(log_top_16, (0, 96))

    # 下面 (16, 96)
    atlas.paste(log_top_16, (16, 96))

    # 2. 頭（ジャック・オ・ランタン）- texOffs(0, 112)
    # サイズ: 16x16x16ピクセルのボックス
    print("頭のテクスチャを配置中...")

    head_face_16 = head_face.resize((16, 16), Image.Resampling.NEAREST)
    head_side_16 = head_side.resize((16, 16), Image.Resampling.NEAREST)
    head_top_16 = head_top.resize((16, 16), Image.Resampling.NEAREST)

    # 前面（顔） (0, 112)
    atlas.paste(head_face_16, (0, 112))

    # 右側面 (16, 112)
    atlas.paste(head_side_16, (16, 112))

    # 後面 (32, 112)
    atlas.paste(head_side_16, (32, 112))

    # 左側面 (48, 112)
    atlas.paste(head_side_16, (48, 112))

    # 上面 (0, 128)
    atlas.paste(head_top_16, (0, 128))

    # 下面 (16, 128)
    atlas.paste(head_top_16, (16, 128))

    # 3. 枝 (Branches) - texOffs(64, 0)
    # サイズ: 32x8x8ピクセルのボックス
    print("枝のテクスチャを配置中...")

    log_side_8x8 = log_side.resize((8, 8), Image.Resampling.NEAREST)
    log_side_32x8 = log_side.resize((32, 8), Image.Resampling.NEAREST)

    # 前面 (64, 0) - 32x8
    atlas.paste(log_side_32x8, (64, 0))

    # 右側面 (96, 0) - 8x8
    atlas.paste(log_side_8x8, (96, 0))

    # 後面 (128, 0) - 32x8
    atlas.paste(log_side_32x8, (128, 0))

    # 左側面 (160, 0) - 8x8
    atlas.paste(log_side_8x8, (160, 0))

    # 上面 (64, 8) - 32x8
    log_top_32x8 = log_top.resize((32, 8), Image.Resampling.NEAREST)
    atlas.paste(log_top_32x8, (64, 8))

    # 下面 (96, 8) - 32x8
    atlas.paste(log_top_32x8, (96, 8))

    # 4. 葉 (Leaves) - texOffs(64, 16)
    # サイズ: 16x16x16ピクセルのボックス
    print("葉のテクスチャを配置中...")

    leaves_16 = leaves.resize((16, 16), Image.Resampling.NEAREST)

    # 前面 (64, 16)
    atlas.paste(leaves_16, (64, 16))

    # 右側面 (80, 16)
    atlas.paste(leaves_16, (80, 16))

    # 後面 (96, 16)
    atlas.paste(leaves_16, (96, 16))

    # 左側面 (112, 16)
    atlas.paste(leaves_16, (112, 16))

    # 上面 (64, 32)
    atlas.paste(leaves_16, (64, 32))

    # 下面 (80, 32)
    atlas.paste(leaves_16, (80, 32))

    return atlas


def main():
    print("Tatari Tree テクスチャアトラス生成開始...")
    print()

    # テクスチャアトラスを作成
    atlas = create_texture_atlas()

    if atlas is None:
        print("テクスチャアトラスの生成に失敗しました")
        return

    # 出力ファイル名
    output_file = 'tatari_tree.png'

    # 保存
    atlas.save(output_file, 'PNG')
    print()
    print(f"✓ テクスチャアトラスを生成しました: {output_file}")
    print(f"  サイズ: {atlas.size[0]}x{atlas.size[1]}ピクセル")
    print()
    print("次のステップ:")
    print(f"  {output_file} を以下のディレクトリにコピーしてください:")
    print("  src/main/resources/assets/kamigami/textures/entity/tatari_tree/")


if __name__ == '__main__':
    main()
