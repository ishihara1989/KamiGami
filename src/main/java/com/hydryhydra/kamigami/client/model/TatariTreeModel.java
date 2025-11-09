package com.hydryhydra.kamigami.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * カスタムモデル: 祟り木（Tatari Tree）
 *
 * 構造: - 幹: 高さ96ピクセル（6ブロック） - 顔: 高さ64ピクセル（4ブロック）の位置にジャック・オ・ランタン - 左右の枝:
 * 高さ64ピクセルから水平に長さ32ピクセル（2ブロック） - 葉: 枝の先端に5個（上下左右+先端）
 */
public class TatariTreeModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart root;
    private final ModelPart trunk;
    private final ModelPart head;
    private final ModelPart leftBranch;
    private final ModelPart rightBranch;
    private final ModelPart leftLeaves;
    private final ModelPart rightLeaves;

    public TatariTreeModel(ModelPart root) {
        super(root);
        this.root = root;
        this.trunk = root.getChild("trunk");
        this.head = root.getChild("head");
        this.leftBranch = root.getChild("left_branch");
        this.rightBranch = root.getChild("right_branch");
        this.leftLeaves = this.leftBranch.getChild("left_leaves");
        this.rightLeaves = this.rightBranch.getChild("right_leaves");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        // 幹 (Trunk): 16x96x16 (1x6x1 blocks)
        // 中心に配置、底面から上に伸びる
        PartDefinition trunk = partdefinition.addOrReplaceChild("trunk", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8.0F, -96.0F, -8.0F, 16.0F, 96.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 24.0F, 0.0F)); // 地面の高さ

        // 頭（ジャック・オ・ランタン）: 16x16x16 (1x1x1 block)
        // 高さ64ピクセル（4ブロック）の位置、正面に1.5ブロック出っ張る
        PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 112) // 別のテクスチャ領域
                .addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, -40.0F, -16.0F)); // Y=-40 (高さ4ブロック), Z=-16 (正面に1.5ブロック出っ張る)

        // 左の枝: 32x8x8 (2x0.5x0.5 blocks)
        // 高さ64ピクセルの位置から左に伸びる
        PartDefinition leftBranch = partdefinition.addOrReplaceChild("left_branch", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-32.0F, -4.0F, -4.0F, 32.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-8.0F, -40.0F, 0.0F)); // 幹の左側から伸びる

        // 右の枝: 32x8x8
        PartDefinition rightBranch = partdefinition.addOrReplaceChild("right_branch", CubeListBuilder.create()
                .texOffs(64, 0).addBox(0.0F, -4.0F, -4.0F, 32.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offset(8.0F, -40.0F, 0.0F)); // 幹の右側から伸びる

        // 左の葉（5個のキューブで構成）
        PartDefinition leftLeaves = leftBranch.addOrReplaceChild("left_leaves", CubeListBuilder.create(),
                PartPose.offset(-32.0F, 0.0F, 0.0F)); // 枝の先端

        // 中心の葉
        leftLeaves.addOrReplaceChild("center", CubeListBuilder.create().texOffs(64, 16).addBox(-8.0F, -8.0F, -8.0F,
                16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        // 上の葉
        leftLeaves.addOrReplaceChild("top", CubeListBuilder.create().texOffs(64, 16).addBox(-8.0F, -8.0F, -8.0F, 16.0F,
                16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -16.0F, 0.0F));

        // 下の葉
        leftLeaves.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(64, 16).addBox(-8.0F, -8.0F, -8.0F,
                16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 16.0F, 0.0F));

        // 前の葉
        leftLeaves.addOrReplaceChild("front", CubeListBuilder.create().texOffs(64, 16).addBox(-8.0F, -8.0F, -8.0F,
                16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -16.0F));

        // 後ろの葉
        leftLeaves.addOrReplaceChild("back", CubeListBuilder.create().texOffs(64, 16).addBox(-8.0F, -8.0F, -8.0F, 16.0F,
                16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 16.0F));

        // 右の葉（左と同じ構造）
        PartDefinition rightLeaves = rightBranch.addOrReplaceChild("right_leaves", CubeListBuilder.create(),
                PartPose.offset(32.0F, 0.0F, 0.0F)); // 枝の先端

        rightLeaves.addOrReplaceChild("center", CubeListBuilder.create().texOffs(64, 16).addBox(-8.0F, -8.0F, -8.0F,
                16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        rightLeaves.addOrReplaceChild("top", CubeListBuilder.create().texOffs(64, 16).addBox(-8.0F, -8.0F, -8.0F, 16.0F,
                16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -16.0F, 0.0F));

        rightLeaves.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(64, 16).addBox(-8.0F, -8.0F, -8.0F,
                16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 16.0F, 0.0F));

        rightLeaves.addOrReplaceChild("front", CubeListBuilder.create().texOffs(64, 16).addBox(-8.0F, -8.0F, -8.0F,
                16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, -16.0F));

        rightLeaves.addOrReplaceChild("back", CubeListBuilder.create().texOffs(64, 16).addBox(-8.0F, -8.0F, -8.0F,
                16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 16.0F));

        return LayerDefinition.create(meshdefinition, 256, 256);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        // 移動しないので、特にアニメーションは不要
        // 方向転換は自動的に処理される（エンティティのyRotが適用される）
    }
}
