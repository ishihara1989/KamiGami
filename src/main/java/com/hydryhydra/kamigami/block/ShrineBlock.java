package com.hydryhydra.kamigami.block;

import com.hydryhydra.kamigami.KamiGami;
import com.hydryhydra.kamigami.block.entity.ShrineBlockEntity;
import com.hydryhydra.kamigami.entity.TatariSlimeEntity;
import com.hydryhydra.kamigami.offering.ActionContext;
import com.hydryhydra.kamigami.offering.ShrineOfferingRecipe;
import com.hydryhydra.kamigami.offering.ShrineOfferingRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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

                // 御神体かどうかを判定
                boolean hasSwampDeityCharm = !storedItem.isEmpty()
                        && storedItem.is(KamiGami.CHARM_OF_SWAMP_DEITY.get());
                boolean hasFertilityCharm = !storedItem.isEmpty() && storedItem.is(KamiGami.CHARM_OF_FERTILITY.get());

                KamiGami.LOGGER.info("Deity check - Swamp: {}, Fertility: {}, item: {}", hasSwampDeityCharm,
                        hasFertilityCharm, storedItem.isEmpty() ? "EMPTY" : storedItem.getItem().toString());

                // シルクタッチチェック
                ItemStack tool = player.getMainHandItem();
                boolean hasSilkTouch = tool.getEnchantmentLevel(level.holderOrThrow(Enchantments.SILK_TOUCH)) > 0;

                // 祟りが起きるかどうかを判定
                boolean willCurse = !hasSilkTouch;
                boolean isDeityCharm = hasSwampDeityCharm || hasFertilityCharm;

                // アイテムドロップの処理
                // 御神体の場合：シルクタッチありならドロップ、シルクタッチなし（祟り発生）なら消費
                // 一般アイテムの場合：常にドロップ
                if (!storedItem.isEmpty()) {
                    if (isDeityCharm && willCurse) {
                        // 御神体が祟りを起こす場合は消費（ドロップしない）
                        KamiGami.LOGGER.info("Deity charm consumed by curse (not dropped): {}", storedItem.getItem());
                    } else {
                        // それ以外の場合はドロップ
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), storedItem);
                        KamiGami.LOGGER.info("Item dropped: {}", storedItem.getItem());
                    }
                }

                // 祟りの処理（シルクタッチなしの場合のみ）
                if (willCurse && level instanceof ServerLevel serverLevel) {
                    // レシピシステムを使って祟りを処理
                    if (hasSwampDeityCharm || hasFertilityCharm) {
                        if (hasSwampDeityCharm) {
                            KamiGami.LOGGER.info(
                                    "Swamp Deity Shrine destroyed without Silk Touch at {} - curse activated", pos);
                        } else {
                            KamiGami.LOGGER.info(
                                    "Fertility Deity Shrine destroyed without Silk Touch at {} - curse activated", pos);
                        }
                        // レシピシステムを使って御神体の祟りを処理
                        executeRecipeOrFallback(serverLevel, pos, player, storedItem);
                    } else {
                        KamiGami.LOGGER.info("Normal Shrine destroyed without Silk Touch at {} - minor curse activated",
                                pos);
                        // レシピシステムを使って通常の祟りを処理
                        executeRecipeOrFallback(serverLevel, pos, player, storedItem);
                    }
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * レシピシステムを使って祟りを実行する。 マッチするレシピがない場合はフォールバックメソッドを呼び出す。
     *
     * @param level
     *            サーバーレベル
     * @param pos
     *            祠の座標
     * @param player
     *            破壊したプレイヤー
     * @param storedItem
     *            祠に格納されていたアイテム
     */
    private void executeRecipeOrFallback(ServerLevel level, BlockPos pos, Player player, ItemStack storedItem) {
        // 通常の祠（空のアイテム）の場合は直接レシピを取得
        // NOTE: Ingredient.of() は空を許可しないため、findRecipe() は使えない
        var recipeOpt = storedItem.isEmpty()
                ? ShrineOfferingRecipes.getNormalShrineCurseRecipe()
                : ShrineOfferingRecipes.findRecipe(ShrineOfferingRecipe.TriggerType.ON_BREAK, storedItem);

        if (recipeOpt.isPresent()) {
            var recipe = recipeOpt.get().recipe();
            KamiGami.LOGGER.info("Executing shrine offering recipe: {}", recipeOpt.get().id());

            // ActionContextを作成
            RandomSource random = level.getRandom();
            ActionContext ctx = new ActionContext(level, pos, player, storedItem, random);

            // レシピのアクションを実行
            boolean success = recipe.actions().perform(ctx);
            if (!success) {
                KamiGami.LOGGER.warn("Recipe execution returned false: {}", recipeOpt.get().id());
            }
        } else {
            // レシピが見つからない場合はフォールバック
            KamiGami.LOGGER.warn("No recipe found for shrine offering, using fallback");
            handleNormalShrineCurse(level, pos);
        }
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

}
