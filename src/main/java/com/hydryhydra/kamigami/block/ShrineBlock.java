package com.hydryhydra.kamigami.block;

import com.hydryhydra.kamigami.KamiGami;
import com.hydryhydra.kamigami.block.entity.ShrineBlockEntity;
import com.hydryhydra.kamigami.entity.TatariSlimeEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShrineBlock extends BaseEntityBlock {
    public static final MapCodec<ShrineBlock> CODEC = simpleCodec(ShrineBlock::new);
    // VoxelShape for the block (approximate shape for a shrine with triangular
    // roof)
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 12, 14), // Base (shrine body)
            Block.box(1, 12, 1, 15, 16, 15) // Roof
    );

    public ShrineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShrineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> blockEntityType) {
        // サーバー側でのみTickを実行
        return level.isClientSide()
                ? null
                : createTickerHelper(blockEntityType, KamiGami.SHRINE_BLOCK_ENTITY.get(),
                        ShrineBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        // If player is sneaking, reverse the direction (180 degrees rotation)
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            facing = facing.getOpposite();
        }
        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ShrineBlockEntity shrineEntity)) {
            return InteractionResult.PASS;
        }
        ItemStack storedItem = shrineEntity.getStoredItem();
        if (!stack.isEmpty() && storedItem.isEmpty()) {
            if (!level.isClientSide()) {
                ItemStack toStore = stack.copy();
                toStore.setCount(1);
                shrineEntity.setStoredItem(toStore);
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
        }
        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ShrineBlockEntity shrineEntity)) {
            return InteractionResult.PASS;
        }
        ItemStack storedItem = shrineEntity.getStoredItem();
        if (storedItem.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack toGive = storedItem.copy();
        boolean inserted = false;
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, toGive);
            inserted = true;
        } else if (player.getInventory().add(toGive)) {
            inserted = true;
        }
        if (!inserted) {
            player.drop(toGive, false);
        }
        shrineEntity.setStoredItem(ItemStack.EMPTY);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state,
            net.minecraft.world.entity.player.Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ShrineBlockEntity shrineEntity) {
            if (!level.isClientSide()) {
                ItemStack storedItem = shrineEntity.getStoredItem();

                // ログ: アイテムドロップ前の状態
                KamiGami.LOGGER.info("Shrine being destroyed at {}, stored item before drop: {} ({})", pos,
                        storedItem.getItem(), storedItem.isEmpty() ? "EMPTY" : "count=" + storedItem.getCount());

                // 重要: ドロップ前に御神体かどうかを判定して保存
                boolean hasSwampDeityCharm = !storedItem.isEmpty()
                        && storedItem.is(KamiGami.CHARM_OF_SWAMP_DEITY.get());

                KamiGami.LOGGER.info("Pre-drop deity check - has Swamp Deity charm: {}, item: {}", hasSwampDeityCharm,
                        storedItem.isEmpty() ? "EMPTY" : storedItem.getItem().toString());

                // 重要: ドロップ前に豊穣の御神体かどうかも判定して保存
                boolean hasFertilityCharm = !storedItem.isEmpty() && storedItem.is(KamiGami.CHARM_OF_FERTILITY.get());
                KamiGami.LOGGER.info("Pre-drop deity check - has Fertility Deity charm: {}, item: {}",
                        hasFertilityCharm, storedItem.isEmpty() ? "EMPTY" : storedItem.getItem().toString());

                // アイテムドロップ
                if (!storedItem.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), storedItem);
                }

                // シルクタッチチェック：シルクタッチで破壊されていない場合、Tatariを召喚
                ItemStack tool = player.getMainHandItem();
                boolean hasSilkTouch = tool.getEnchantmentLevel(level.holderOrThrow(Enchantments.SILK_TOUCH)) > 0;

                if (!hasSilkTouch && level instanceof ServerLevel serverLevel) {
                    // ドロップ前に保存した判定結果を使用
                    if (hasSwampDeityCharm) {
                        KamiGami.LOGGER.info("Swamp Deity Shrine destroyed without Silk Touch at {}", pos);
                        handleSwampDeityShrineCurse(serverLevel, pos);
                    } else if (hasFertilityCharm) {
                        KamiGami.LOGGER.info("Fertility Deity Shrine destroyed without Silk Touch at {}", pos);
                        handleFertilityDeityShrineCurse(serverLevel, pos);
                    } else {
                        KamiGami.LOGGER.info("Normal Shrine destroyed without Silk Touch at {}", pos);
                        handleNormalShrineCurse(serverLevel, pos);
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * 沼の神の御神体が入った祠をシルクタッチなしで破壊したときの処理
     *
     * 1. 周囲5x5、祠から1段下（地面）から±1マスの原木を削除 2. 空いたマスに粘土・土・苔をランダムに40%の確率で生成 3.
     * 植物があれば骨粉に変換してドロップ 4. サイズ4のスライムを召喚 5. 爆発音と爆発エフェクト
     */
    private void handleSwampDeityShrineCurse(ServerLevel level, BlockPos shrinePos) {
        RandomSource random = level.getRandom();

        // 爆発音と爆発エフェクト
        level.playSound(null, shrinePos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, shrinePos.getX() + 0.5, shrinePos.getY() + 0.5,
                shrinePos.getZ() + 0.5, 1, 0, 0, 0, 0);

        int logsRemoved = 0;
        int plantsConverted = 0;
        int blocksPlaced = 0;

        // 周囲5x5、祠から1段下（地面）から±1マスの範囲を処理
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 0; dy++) { // 祠から見て-2（地面-1）から0（祠の高さ）
                    BlockPos checkPos = shrinePos.offset(dx, dy, dz);
                    BlockState checkState = level.getBlockState(checkPos);

                    // 原木を削除（タグベースで判定）
                    if (checkState.is(BlockTags.LOGS)) {
                        level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                        logsRemoved++;
                        KamiGami.LOGGER.debug("Swamp Curse: Removed log at {} ({})", checkPos, checkState.getBlock());
                    }

                    // 植物を骨粉に変換
                    if (isPlant(checkState)) {
                        level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                        Containers.dropItemStack(level, checkPos.getX(), checkPos.getY(), checkPos.getZ(),
                                new ItemStack(Items.BONE_MEAL, 1));
                        plantsConverted++;
                        KamiGami.LOGGER.debug("Swamp Curse: Converted plant at {} to bone meal", checkPos);
                    }

                    // 空いたマスに粘土・土・苔をランダムに40%の確率で生成
                    if (checkState.isAir() && (dy < 0 || random.nextFloat() < 0.4F)) {
                        BlockState blockToPlace = switch (random.nextInt(3)) {
                            case 0 -> Blocks.CLAY.defaultBlockState();
                            case 1 -> Blocks.DIRT.defaultBlockState();
                            default -> Blocks.MOSS_BLOCK.defaultBlockState();
                        };
                        level.setBlock(checkPos, blockToPlace, 3);
                        blocksPlaced++;
                        KamiGami.LOGGER.debug("Swamp Curse: Placed {} at {}", blockToPlace.getBlock(), checkPos);
                    }
                }
            }
        }

        // サイズ4のスライムを召喚
        TatariSlimeEntity tatariSlime = KamiGami.TATARI_SLIME.get().create(level, EntitySpawnReason.TRIGGERED);
        if (tatariSlime != null) {
            tatariSlime.setPos(shrinePos.getX() + 0.5, shrinePos.getY() + 0.5, shrinePos.getZ() + 0.5);
            // 重要: addFreshEntityの前にサイズを設定（finalizeSpawnで上書きされないように）
            tatariSlime.setSize(4, true); // サイズ4で召喚
            KamiGami.LOGGER.info("Swamp Curse: Setting size to 4 before adding to world");
            level.addFreshEntity(tatariSlime);
            KamiGami.LOGGER.info("Swamp Curse: Summoned size {} Tatari Slime at {} (after addFreshEntity)",
                    tatariSlime.getSize(), shrinePos);
        }

        KamiGami.LOGGER.info("Swamp Curse completed: {} logs removed, {} plants converted, {} blocks placed",
                logsRemoved, plantsConverted, blocksPlaced);
    }

    /**
     * 豊穣の御神体が入った祠をシルクタッチなしで破壊したときの処理
     *
     * 1. 周囲5x5の範囲にランダムにlog、Podzol、Gravel、Sand、荒れた土をばらまく - 祠と同じ高さは40%の確率 -
     * それ以下の高さは100%の確率 2. Tatari of Fertility Deityを召喚 3. 爆発音と爆発エフェクト
     */
    private void handleFertilityDeityShrineCurse(ServerLevel level, BlockPos shrinePos) {
        RandomSource random = level.getRandom();

        // 爆発音と爆発エフェクト
        level.playSound(null, shrinePos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.5F, 0.8F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, shrinePos.getX() + 0.5, shrinePos.getY() + 0.5,
                shrinePos.getZ() + 0.5, 1, 0, 0, 0, 0);

        int blocksPlaced = 0;

        // 周囲5x5、祠の高さ以下の範囲にブロックをばらまく
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                // 祠の位置自体はスキップ
                if (dx == 0 && dz == 0) {
                    continue;
                }

                // Y座標の範囲: 祠の高さ（dy=0）から下（dy=-2）まで
                for (int dy = 0; dy >= -2; dy--) {
                    BlockPos targetPos = shrinePos.offset(dx, dy, dz);
                    BlockState currentState = level.getBlockState(targetPos);

                    // 既にブロックがある場合はスキップ（空気ブロックのみに配置）
                    if (!currentState.isAir()) {
                        continue;
                    }

                    // 配置確率: 祠と同じ高さ（dy=0）は40%、それ以下（dy<0）は100%
                    float placementChance = (dy == 0) ? 0.4F : 1.0F;
                    if (random.nextFloat() >= placementChance) {
                        continue;
                    }

                    // ランダムにブロックを選択
                    BlockState blockToPlace = switch (random.nextInt(5)) {
                        case 0 -> Blocks.OAK_LOG.defaultBlockState();
                        case 1 -> Blocks.PODZOL.defaultBlockState();
                        case 2 -> Blocks.GRAVEL.defaultBlockState();
                        case 3 -> Blocks.SAND.defaultBlockState();
                        default -> Blocks.COARSE_DIRT.defaultBlockState(); // 荒れた土
                    };

                    level.setBlock(targetPos, blockToPlace, 3);
                    blocksPlaced++;
                    KamiGami.LOGGER.debug("Fertility Curse: Placed {} at {}", blockToPlace.getBlock(), targetPos);
                }
            }
        }

        // Tatari of Fertility Deityを召喚
        com.hydryhydra.kamigami.entity.TatariFertilityEntity tatariFertility = KamiGami.TATARI_FERTILITY.get()
                .create(level, EntitySpawnReason.TRIGGERED);
        if (tatariFertility != null) {
            tatariFertility.setPos(shrinePos.getX() + 0.5, shrinePos.getY(), shrinePos.getZ() + 0.5);
            level.addFreshEntity(tatariFertility);
            KamiGami.LOGGER.info("Fertility Curse: Summoned Tatari of Fertility Deity at {}", shrinePos);
        }

        KamiGami.LOGGER.info("Fertility Curse completed: {} blocks placed", blocksPlaced);
    }

    /**
     * 通常の祠をシルクタッチなしで破壊したときの処理
     *
     * 1. 小さめの爆発音と爆発エフェクト 2. サイズ1のスライムを召喚
     */
    private void handleNormalShrineCurse(ServerLevel level, BlockPos shrinePos) {
        // 小さめの爆発音と爆発エフェクト
        level.playSound(null, shrinePos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.5F, 1.2F);
        level.sendParticles(ParticleTypes.EXPLOSION, shrinePos.getX() + 0.5, shrinePos.getY() + 0.5,
                shrinePos.getZ() + 0.5, 1, 0, 0, 0, 0);

        // サイズ1のスライムを召喚
        TatariSlimeEntity tatariSlime = KamiGami.TATARI_SLIME.get().create(level, EntitySpawnReason.TRIGGERED);
        if (tatariSlime != null) {
            tatariSlime.setPos(shrinePos.getX() + 0.5, shrinePos.getY() + 0.5, shrinePos.getZ() + 0.5);
            // addFreshEntityの前にサイズを設定（finalizeSpawnで上書きされないように）
            tatariSlime.setSize(1, true); // サイズ1で召喚
            KamiGami.LOGGER.info("Normal Shrine curse: Setting size to 1 before adding to world");
            level.addFreshEntity(tatariSlime);
            KamiGami.LOGGER.info("Normal Shrine curse: Summoned size {} Tatari Slime at {} (after addFreshEntity)",
                    tatariSlime.getSize(), shrinePos);
        }
    }

    /**
     * ブロックが植物かどうか判定（タグベース）
     *
     * 以下のいずれかに該当する場合に植物と判定: - 骨粉が使えるブロック (BonemealableBlock) - 花タグを持つ - 小さい花タグを持つ -
     * 苗木タグを持つ - 作物タグを持つ
     *
     * 除外: - 草ブロック (GRASS_BLOCK) - 菌糸 (MYCELIUM) - ポドゾル (PODZOL) - 土系ブロック
     */
    private boolean isPlant(BlockState state) {
        // 草ブロックや土系ブロックは除外
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.PODZOL)
                || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
            return false;
        }

        // 骨粉が使えるか
        if (state.getBlock() instanceof BonemealableBlock) {
            return true;
        }

        // 植物系タグをチェック
        return state.is(BlockTags.FLOWERS) || state.is(BlockTags.SMALL_FLOWERS) || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CROPS)
                // サボテン、サトウキビ、竹などの追加判定
                || state.is(Blocks.CACTUS) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO)
                || state.is(Blocks.VINE) || state.is(Blocks.LILY_PAD) || state.is(Blocks.SEAGRASS)
                || state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.KELP) || state.is(Blocks.KELP_PLANT);
    }
}
