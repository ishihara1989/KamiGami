package com.hydryhydra.kamigami;

import org.slf4j.Logger;

import com.hydryhydra.kamigami.block.ShrineBlock;
import com.hydryhydra.kamigami.block.entity.ShrineBlockEntity;
import com.hydryhydra.kamigami.entity.FertilityFireballEntity;
import com.hydryhydra.kamigami.entity.PaperChickenEntity;
import com.hydryhydra.kamigami.entity.PaperCowEntity;
import com.hydryhydra.kamigami.entity.PaperSheepEntity;
import com.hydryhydra.kamigami.entity.SwampTatariSlimeEntity;
import com.hydryhydra.kamigami.entity.TatariFertilityEntity;
import com.hydryhydra.kamigami.entity.TatariSlimeEntity;
import com.hydryhydra.kamigami.item.CharmOfFertilityItem;
import com.hydryhydra.kamigami.item.CharmOfSwampDeityItem;
import com.hydryhydra.kamigami.item.ShikigamiSummonItem;
import com.hydryhydra.kamigami.offering.ShrineOfferingRecipes;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(KamiGami.MODID)
public class KamiGami {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "kamigami";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Items which will all be registered under
    // the "kamigami" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold Blocks which will all be registered under
    // the "kamigami" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold BlockEntityTypes which will all be
    // registered
    // under the "kamigami" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister
            .create(Registries.BLOCK_ENTITY_TYPE, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be
    // registered under the "kamigami" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create a Deferred Register to hold EntityTypes which will all be registered
    // under the "kamigami" namespace
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE,
            MODID);

    // Register Shikigami entities
    public static final DeferredHolder<EntityType<?>, EntityType<PaperCowEntity>> PAPER_COW = ENTITY_TYPES.register(
            "paper_cow",
            () -> EntityType.Builder.of(PaperCowEntity::new, MobCategory.CREATURE).sized(0.9F, 1.4F)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(MODID, "paper_cow"))));

    public static final DeferredHolder<EntityType<?>, EntityType<PaperChickenEntity>> PAPER_CHICKEN = ENTITY_TYPES
            .register("paper_chicken",
                    () -> EntityType.Builder.of(PaperChickenEntity::new, MobCategory.CREATURE).sized(0.4F, 0.7F)
                            .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    ResourceLocation.fromNamespaceAndPath(MODID, "paper_chicken"))));

    public static final DeferredHolder<EntityType<?>, EntityType<PaperSheepEntity>> PAPER_SHEEP = ENTITY_TYPES.register(
            "paper_sheep",
            () -> EntityType.Builder.of(PaperSheepEntity::new, MobCategory.CREATURE).sized(0.9F, 1.3F)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE,
                            ResourceLocation.fromNamespaceAndPath(MODID, "paper_sheep"))));

    // Register Tatari Slime (hostile mob)
    public static final DeferredHolder<EntityType<?>, EntityType<TatariSlimeEntity>> TATARI_SLIME = ENTITY_TYPES
            .register("tatari_slime",
                    () -> EntityType.Builder.of(TatariSlimeEntity::new, MobCategory.MONSTER).sized(0.52F, 0.52F)
                            .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    ResourceLocation.fromNamespaceAndPath(MODID, "tatari_slime"))));

    // Register Swamp Tatari Slime (hostile mob - size 4, no splitting)
    public static final DeferredHolder<EntityType<?>, EntityType<SwampTatariSlimeEntity>> SWAMP_TATARI_SLIME = ENTITY_TYPES
            .register("swamp_tatari_slime",
                    () -> EntityType.Builder.of(SwampTatariSlimeEntity::new, MobCategory.MONSTER).sized(2.04F, 2.04F)
                            .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    ResourceLocation.fromNamespaceAndPath(MODID, "swamp_tatari_slime"))));

    // Register Tatari Fertility Deity (hostile mob)
    public static final DeferredHolder<EntityType<?>, EntityType<TatariFertilityEntity>> TATARI_FERTILITY = ENTITY_TYPES
            .register("tatari_fertility",
                    () -> EntityType.Builder.of(TatariFertilityEntity::new, MobCategory.MONSTER).sized(1.4F, 4.0F)
                            .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    ResourceLocation.fromNamespaceAndPath(MODID, "tatari_fertility"))));

    // Register Fertility Fireball (projectile)
    public static final DeferredHolder<EntityType<?>, EntityType<FertilityFireballEntity>> FERTILITY_FIREBALL = ENTITY_TYPES
            .register("fertility_fireball",
                    () -> EntityType.Builder.<FertilityFireballEntity>of(FertilityFireballEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(4).updateInterval(10)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                                    ResourceLocation.fromNamespaceAndPath(MODID, "fertility_fireball"))));

    // Register Shrine block
    public static final DeferredBlock<ShrineBlock> SHRINE = BLOCKS.register("shrine",
            () -> new ShrineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 3.0F)
                    .sound(SoundType.WOOD).noOcclusion().setId(ResourceKey.create(Registries.BLOCK,
                            ResourceLocation.fromNamespaceAndPath(MODID, "shrine")))));

    // Register Shrine BlockEntity
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShrineBlockEntity>> SHRINE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES
            .register("shrine", () -> new BlockEntityType<>(ShrineBlockEntity::new, SHRINE.get()));

    // Register Shikigami core item (material for summoning items)
    public static final DeferredItem<Item> SHIKIGAMI_CORE = ITEMS.registerSimpleItem("shikigami_core");

    // Register Shikigami summoning items
    public static final DeferredItem<Item> PAPER_COW_SUMMON = ITEMS.registerItem("paper_cow_summon",
            properties -> new ShikigamiSummonItem(properties.stacksTo(16), () -> PAPER_COW.get()));

    public static final DeferredItem<Item> PAPER_CHICKEN_SUMMON = ITEMS.registerItem("paper_chicken_summon",
            properties -> new ShikigamiSummonItem(properties.stacksTo(16), () -> PAPER_CHICKEN.get()));

    public static final DeferredItem<Item> PAPER_SHEEP_SUMMON = ITEMS.registerItem("paper_sheep_summon",
            properties -> new ShikigamiSummonItem(properties.stacksTo(16), () -> PAPER_SHEEP.get()));

    // Register Shrine block item
    public static final DeferredItem<BlockItem> SHRINE_ITEM = ITEMS.registerItem("shrine",
            properties -> new BlockItem(SHRINE.get(), properties.useBlockDescriptionPrefix()));

    // Register Charm of Swamp Deity (御神体アイテム)
    public static final DeferredItem<Item> CHARM_OF_SWAMP_DEITY = ITEMS.registerItem("charm_of_swamp_deity",
            properties -> new CharmOfSwampDeityItem(properties.stacksTo(1)));

    // Register Charm of Fertility (豊穣の御神体)
    public static final DeferredItem<Item> CHARM_OF_FERTILITY = ITEMS.registerItem("charm_of_fertility",
            properties -> new CharmOfFertilityItem(properties.stacksTo(1)));

    // Creates a creative tab with the id "kamigami:kamigami_tab" for KamiGami items
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> KAMIGAMI_TAB = CREATIVE_MODE_TABS.register(
            "kamigami_tab",
            () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.kamigami"))
                    .withTabsBefore(CreativeModeTabs.COMBAT).icon(() -> PAPER_COW_SUMMON.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(SHRINE_ITEM.get());
                        output.accept(CHARM_OF_SWAMP_DEITY.get());
                        output.accept(CHARM_OF_FERTILITY.get());
                        output.accept(PAPER_COW_SUMMON.get());
                        output.accept(PAPER_CHICKEN_SUMMON.get());
                        output.accept(PAPER_SHEEP_SUMMON.get());
                    }).build());

    // The constructor for the mod class is the first code that is run when your mod
    // is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and
    // pass them in automatically.
    public KamiGami(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entities get
        // registered
        BLOCK_ENTITY_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so entities get
        // registered
        ENTITY_TYPES.register(modEventBus);

        // Register entity attributes
        modEventBus.addListener(this::registerEntityAttributes);

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM KAMIGAMI MOD COMMON SETUP");

        // Register shrine offering recipes
        event.enqueueWork(ShrineOfferingRecipes::registerDefaultRecipes);
    }

    // Register entity attributes
    private void registerEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(PAPER_COW.get(), PaperCowEntity.createAttributes().build());
        event.put(PAPER_CHICKEN.get(), PaperChickenEntity.createAttributes().build());
        event.put(PAPER_SHEEP.get(), PaperSheepEntity.createAttributes().build());
        event.put(TATARI_SLIME.get(), TatariSlimeEntity.createAttributes().build());
        event.put(SWAMP_TATARI_SLIME.get(), TatariSlimeEntity.createAttributes().build()); // 親クラスの属性を使用
        event.put(TATARI_FERTILITY.get(), TatariFertilityEntity.createAttributes().build());
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from KamiGami mod server starting");
    }
}
