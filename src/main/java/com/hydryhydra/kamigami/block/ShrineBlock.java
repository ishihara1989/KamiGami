package com.hydryhydra.kamigami.block;

import com.hydryhydra.kamigami.KamiGami;
import com.hydryhydra.kamigami.block.entity.ShrineBlockEntity;
import com.hydryhydra.kamigami.curse.ActionContext;
import com.hydryhydra.kamigami.curse.ShrineCurseRecipe;
import com.hydryhydra.kamigami.curse.ShrineCurseRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        // 火の神の御神体が祠に入っている場合、明るさ15を発する
        if (level.getBlockEntity(pos) instanceof ShrineBlockEntity shrineEntity) {
            ItemStack storedItem = shrineEntity.getStoredItem();
            if (!storedItem.isEmpty() && storedItem.is(KamiGami.CHARM_OF_FIRE_DEITY.get())) {
                return 15;
            }
        }
        return 0;
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
                KamiGami.LOGGER.info("Shrine being destroyed at {}, stored item: {}", pos,
                        storedItem.isEmpty() ? "EMPTY" : storedItem.getItem().toString());

                // シルクタッチチェック
                ItemStack tool = player.getMainHandItem();
                boolean hasSilkTouch = tool.getEnchantmentLevel(level.holderOrThrow(Enchantments.SILK_TOUCH)) > 0;

                // レシピ検索
                var recipeOpt = ShrineCurseRecipes.findRecipe(ShrineCurseRecipe.TriggerType.ON_BREAK, storedItem);

                // 祟りが起きるかどうかを判定（レシピが見つかり、かつシルクタッチなし）
                boolean willCurse = recipeOpt.isPresent() && recipeOpt.get().recipe().requireNoSilkTouch()
                        && !hasSilkTouch;

                // アイテムドロップの処理
                if (!storedItem.isEmpty()) {
                    if (willCurse) {
                        // 祟りが起きる場合は消費（ドロップしない）
                        KamiGami.LOGGER.info("Item consumed by curse (not dropped): {}", storedItem.getItem());
                    } else {
                        // それ以外の場合はドロップ
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), storedItem);
                        KamiGami.LOGGER.info("Item dropped: {}", storedItem.getItem());
                    }
                }

                // 祟りの処理
                if (willCurse && level instanceof ServerLevel serverLevel) {
                    KamiGami.LOGGER.info("Shrine destroyed without Silk Touch at {} - executing curse recipe: {}", pos,
                            recipeOpt.get().id());
                    executeRecipe(serverLevel, pos, player, storedItem, recipeOpt.get());
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * レシピシステムを使って祟りを実行する。
     *
     * @param level
     *            サーバーレベル
     * @param pos
     *            祠の座標
     * @param player
     *            破壊したプレイヤー
     * @param storedItem
     *            祠に格納されていたアイテム
     * @param loadedRecipe
     *            実行するレシピ
     */
    private void executeRecipe(ServerLevel level, BlockPos pos, Player player, ItemStack storedItem,
            ShrineCurseRecipes.LoadedRecipe loadedRecipe) {
        var recipe = loadedRecipe.recipe();
        KamiGami.LOGGER.info("Executing shrine curse recipe: {}", loadedRecipe.id());

        // ActionContextを作成
        RandomSource random = level.getRandom();
        ActionContext ctx = new ActionContext(level, pos, player, storedItem, random);

        // レシピのアクションを実行
        boolean success = recipe.actions().perform(ctx);
        if (!success) {
            KamiGami.LOGGER.warn("Recipe execution returned false: {}", loadedRecipe.id());
        }
    }

}
