package com.hydryhydra.kamigami.block.entity;

import com.hydryhydra.kamigami.KamiGami;
import com.mojang.serialization.DataResult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ShrineBlockEntity extends BlockEntity {
    private ItemStack storedItem = ItemStack.EMPTY;
    private int tickCounter = 0;
    // きのこ成長の間隔（100tick = 5秒）
    private static final int MUSHROOM_GROW_INTERVAL = 100;

    public ShrineBlockEntity(BlockPos pos, BlockState blockState) {
        super(KamiGami.SHRINE_BLOCK_ENTITY.get(), pos, blockState);
    }

    /**
     * サーバー側のTick処理 御神体がセットされている場合、その効果を発動する
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, ShrineBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.tickCounter++;

        // 御神体の効果をチェック
        ItemStack storedItem = blockEntity.getStoredItem();
        if (storedItem.isEmpty()) {
            return;
        }

        // 沼の神の御神体がセットされている場合
        if (storedItem.is(KamiGami.CHARM_OF_SWAMP_DEITY.get())) {
            // きのこ成長効果
            if (blockEntity.tickCounter >= MUSHROOM_GROW_INTERVAL) {
                blockEntity.tickCounter = 0;
                blockEntity.growMushroomsAroundShrine((ServerLevel) level, pos);
            }

            // パーティクル効果（毎tick 10%の確率で発生）
            if (level.getRandom().nextFloat() < 0.1F) {
                blockEntity.spawnSwampParticles((ServerLevel) level, pos);
            }
        }
    }

    public ItemStack getStoredItem() {
        return this.storedItem;
    }

    public void setStoredItem(ItemStack stack) {
        this.storedItem = stack;
        this.setChanged();
        // Sync to client
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.storedItem.isEmpty()) {
            output.store("StoredItem", ItemStack.CODEC, this.storedItem);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.storedItem = input.read("StoredItem", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    // For client synchronization
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        // Encode ItemStack using CODEC
        if (!this.storedItem.isEmpty()) {
            DataResult<Tag> result = ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),
                    this.storedItem);
            result.ifSuccess(nbtTag -> tag.put("StoredItem", nbtTag));
        }
        return tag;
    }

    /**
     * 祠の周囲3x3マスの土にきのこを生やす
     */
    private void growMushroomsAroundShrine(ServerLevel level, BlockPos shrinePos) {
        RandomSource random = level.getRandom();

        KamiGami.LOGGER.info("Swamp Deity: Attempting to grow mushrooms around shrine at {}", shrinePos);

        int candidateCount = 0;
        int attemptCount = 0;

        // 祠の周囲3x3マス（X: -1~+1, Z: -1~+1）を走査
        // 祠の真下（Y-1）を基準に、その上にきのこを生やす
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                // 祠の位置自体はスキップ
                if (dx == 0 && dz == 0) {
                    continue;
                }

                // チェック位置（祠の真下のY座標）
                BlockPos groundPos = shrinePos.offset(dx, -1, dz);
                BlockState groundState = level.getBlockState(groundPos);

                KamiGami.LOGGER.debug("Swamp Deity: Checking ground at {} = {}", groundPos, groundState.getBlock());

                // 土ブロックの場合
                if (groundState.is(Blocks.DIRT) || groundState.is(Blocks.GRASS_BLOCK) || groundState.is(Blocks.PODZOL)
                        || groundState.is(Blocks.MYCELIUM)) {

                    // その上のブロックをチェック（祠と同じ高さ）
                    BlockPos abovePos = groundPos.above();
                    BlockState aboveState = level.getBlockState(abovePos);

                    KamiGami.LOGGER.debug("Swamp Deity: Above block at {} = {}", abovePos, aboveState.getBlock());

                    candidateCount++;

                    // 上が空気ブロックかチェック
                    if (aboveState.isAir()) {
                        // 光レベルをチェック（0-15、15が最も明るい）
                        int lightLevel = level.getRawBrightness(abovePos, 0);

                        // Myceliumの上は任意の光レベルでOK、それ以外は光レベル12以下
                        boolean canPlant = groundState.is(Blocks.MYCELIUM) || lightLevel <= 12;

                        KamiGami.LOGGER.debug("Swamp Deity: Light level at {} = {}, canPlant = {}", abovePos,
                                lightLevel, canPlant);

                        // 光レベルが適切で、ランダムで成功した場合（20%の確率）
                        if (canPlant && random.nextFloat() < 0.2F) {
                            attemptCount++;
                            // 赤いきのこか茶色いきのこをランダムに生やす
                            BlockState mushroomState = random.nextBoolean()
                                    ? Blocks.RED_MUSHROOM.defaultBlockState()
                                    : Blocks.BROWN_MUSHROOM.defaultBlockState();

                            level.setBlock(abovePos, mushroomState, 3);

                            KamiGami.LOGGER.info("Swamp Deity: Mushroom grown at {} (light level: {})", abovePos,
                                    lightLevel);
                        }
                    }
                }
            }
        }

        KamiGami.LOGGER.info("Swamp Deity: Found {} candidate positions, grew {} mushrooms", candidateCount,
                attemptCount);
    }

    /**
     * 沼っぽいパーティクルを発生させる
     */
    private void spawnSwampParticles(ServerLevel level, BlockPos shrinePos) {
        RandomSource random = level.getRandom();

        // 祠の中心付近にパーティクルを発生
        double x = shrinePos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
        double y = shrinePos.getY() + 0.7; // 祠の少し上
        double z = shrinePos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;

        // ドリップリーフパーティクル（水滴のような沼っぽい効果）
        level.sendParticles(ParticleTypes.DRIPPING_WATER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
