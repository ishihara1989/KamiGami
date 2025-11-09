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
    // きのこ成長の間隔（200tick = 10秒）
    private static final int MUSHROOM_GROW_INTERVAL = 200;
    // 豊穣効果の間隔（200tick = 10秒）
    private static final int FERTILITY_INTERVAL = 200;

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

        // 豊穣の御神体がセットされている場合
        if (storedItem.is(KamiGami.CHARM_OF_FERTILITY.get())) {
            // 豊穣効果（骨粉効果 + 土→草変換）
            if (blockEntity.tickCounter >= FERTILITY_INTERVAL) {
                blockEntity.tickCounter = 0;
                blockEntity.applyFertilityEffectAroundShrine((ServerLevel) level, pos);
            }

            // パーティクル効果（毎tick 10%の確率で発生）
            if (level.getRandom().nextFloat() < 0.1F) {
                blockEntity.spawnFertilityParticles((ServerLevel) level, pos);
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
     * 祠の周囲3x3マスの土にきのこを生やす - クールタイムごとに1箇所ランダムで土/草を菌糸に変換、またはきのこを生やす - 明るさ関係なくきのこを出す
     */
    private void growMushroomsAroundShrine(ServerLevel level, BlockPos shrinePos) {
        RandomSource random = level.getRandom();

        KamiGami.LOGGER.info("Swamp Deity: Attempting mushroom effect around shrine at {}", shrinePos);

        // 候補となる位置をリストアップ
        java.util.List<BlockPos> dirtPositions = new java.util.ArrayList<>();
        java.util.List<BlockPos> mushroomablePositions = new java.util.ArrayList<>();

        // 祠の周囲3x3マス（X: -1~+1, Z: -1~+1）を走査
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                // 祠の位置自体はスキップ
                if (dx == 0 && dz == 0) {
                    continue;
                }

                // チェック位置（祠の真下のY座標）
                BlockPos groundPos = shrinePos.offset(dx, -1, dz);
                BlockState groundState = level.getBlockState(groundPos);

                // 土/草ブロックは菌糸変換候補
                if (groundState.is(Blocks.DIRT) || groundState.is(Blocks.GRASS_BLOCK)) {
                    dirtPositions.add(groundPos);
                }

                // 菌糸/ポドゾルの上にきのこを生やせる候補
                if (groundState.is(Blocks.PODZOL) || groundState.is(Blocks.MYCELIUM)) {
                    BlockPos abovePos = groundPos.above();
                    BlockState aboveState = level.getBlockState(abovePos);
                    if (aboveState.isAir()) {
                        mushroomablePositions.add(abovePos);
                    }
                }
            }
        }

        // 土→菌糸変換を優先、なければきのこを生やす
        if (!dirtPositions.isEmpty()) {
            // ランダムに1箇所の土を菌糸に変換
            BlockPos targetPos = dirtPositions.get(random.nextInt(dirtPositions.size()));
            level.setBlock(targetPos, Blocks.MYCELIUM.defaultBlockState(), 3);
            KamiGami.LOGGER.info("Swamp Deity: Converted dirt/grass to mycelium at {}", targetPos);
        } else if (!mushroomablePositions.isEmpty()) {
            // ランダムに1箇所にきのこを生やす
            BlockPos targetPos = mushroomablePositions.get(random.nextInt(mushroomablePositions.size()));
            BlockState mushroomState = random.nextBoolean()
                    ? Blocks.RED_MUSHROOM.defaultBlockState()
                    : Blocks.BROWN_MUSHROOM.defaultBlockState();
            level.setBlock(targetPos, mushroomState, 3);
            KamiGami.LOGGER.info("Swamp Deity: Mushroom grown at {}", targetPos);
        } else {
            KamiGami.LOGGER.debug("Swamp Deity: No valid positions found for mushroom or mycelium");
        }
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

        // マイセリウムパーティクル（紫色の胞子っぽいパーティクル）
        level.sendParticles(ParticleTypes.MYCELIUM, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * 祠の周囲3x3マスに豊穣効果を適用する - クールタイムごとに1箇所ランダムで骨粉効果を適用、または土を草に変換
     */
    private void applyFertilityEffectAroundShrine(ServerLevel level, BlockPos shrinePos) {
        RandomSource random = level.getRandom();

        KamiGami.LOGGER.info("Fertility: Attempting fertility effect around shrine at {}", shrinePos);

        // 候補となる位置をリストアップ
        java.util.List<BlockPos> dirtPositions = new java.util.ArrayList<>();
        java.util.List<BlockPos> bonemealablePositions = new java.util.ArrayList<>();

        // 祠の周囲3x3マス（X: -1~+1, Z: -1~+1）を走査
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                // 祠の位置自体はスキップ
                if (dx == 0 && dz == 0) {
                    continue;
                }

                // チェック位置（祠の真下のY座標）
                BlockPos groundPos = shrinePos.offset(dx, -1, dz);
                BlockState groundState = level.getBlockState(groundPos);

                // 土ブロックは草変換候補
                if (groundState.is(Blocks.DIRT)) {
                    dirtPositions.add(groundPos);
                }

                // その上のブロックに骨粉効果を適用できる候補
                BlockPos abovePos = groundPos.above();
                BlockState aboveState = level.getBlockState(abovePos);

                if (aboveState.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock bonemealable) {
                    if (bonemealable.isValidBonemealTarget(level, abovePos, aboveState)) {
                        bonemealablePositions.add(abovePos);
                    }
                }
            }
        }

        // 骨粉効果を優先、なければ土→草変換
        if (!bonemealablePositions.isEmpty()) {
            // ランダムに1箇所に骨粉効果を適用
            BlockPos targetPos = bonemealablePositions.get(random.nextInt(bonemealablePositions.size()));
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock bonemealable) {
                bonemealable.performBonemeal(level, random, targetPos, targetState);
                KamiGami.LOGGER.info("Fertility: Applied bonemeal effect at {}", targetPos);

                // 骨粉パーティクルを発生
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetPos.getX() + 0.5, targetPos.getY() + 0.5,
                        targetPos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.0);
            }
        } else if (!dirtPositions.isEmpty()) {
            // ランダムに1箇所の土を草に変換
            BlockPos targetPos = dirtPositions.get(random.nextInt(dirtPositions.size()));
            level.setBlock(targetPos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            KamiGami.LOGGER.info("Fertility: Converted dirt to grass at {}", targetPos);
        } else {
            KamiGami.LOGGER.debug("Fertility: No valid positions found for bonemeal or grass conversion");
        }
    }

    /**
     * 豊穣パーティクルを発生させる（骨粉っぽいパーティクル）
     */
    private void spawnFertilityParticles(ServerLevel level, BlockPos shrinePos) {
        RandomSource random = level.getRandom();

        // 祠の中心付近にパーティクルを発生
        double x = shrinePos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5;
        double y = shrinePos.getY() + 0.7; // 祠の少し上
        double z = shrinePos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5;

        // ハッピービレッジャーパーティクル（骨粉っぽい緑の星）
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }
}
