package donut.utility.features.networth;

import donut.utility.features.networth.ItemData;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class ValuableItems {
    private static final Set<String> VALUABLE_IDS = new HashSet<String>();

    private ValuableItems() {
    }

    public static boolean isValuable(String itemId, String displayName) {
        String lowerItemId = itemId.toLowerCase();
        String lowerName = displayName.toLowerCase();
        if (VALUABLE_IDS.contains(lowerItemId)) {
            if (lowerItemId.equals("tipped_arrow")) {
                return lowerName.contains("arrow of luck");
            }
            return true;
        }
        return lowerItemId.equals("enchanted_book") && lowerName.contains("wind burst");
    }

    public static boolean isValuable(ItemData item) {
        if (item.isSpawner()) {
            return true;
        }
        return ValuableItems.isValuable(item.getItemId(), item.getDisplayName());
    }

    static {
        VALUABLE_IDS.add("diamond_helmet");
        VALUABLE_IDS.add("diamond_chestplate");
        VALUABLE_IDS.add("diamond_leggings");
        VALUABLE_IDS.add("diamond_boots");
        VALUABLE_IDS.add("diamond_sword");
        VALUABLE_IDS.add("diamond_pickaxe");
        VALUABLE_IDS.add("diamond_axe");
        VALUABLE_IDS.add("diamond_shovel");
        VALUABLE_IDS.add("diamond_hoe");
        VALUABLE_IDS.add("netherite_helmet");
        VALUABLE_IDS.add("netherite_chestplate");
        VALUABLE_IDS.add("netherite_leggings");
        VALUABLE_IDS.add("netherite_boots");
        VALUABLE_IDS.add("netherite_sword");
        VALUABLE_IDS.add("netherite_pickaxe");
        VALUABLE_IDS.add("netherite_axe");
        VALUABLE_IDS.add("netherite_shovel");
        VALUABLE_IDS.add("netherite_hoe");
        VALUABLE_IDS.add("mace");
        VALUABLE_IDS.add("trident");
        VALUABLE_IDS.add("crossbow");
        VALUABLE_IDS.add("bow");
        VALUABLE_IDS.add("diamond_block");
        VALUABLE_IDS.add("emerald_block");
        VALUABLE_IDS.add("netherite_block");
        VALUABLE_IDS.add("gold_block");
        VALUABLE_IDS.add("iron_block");
        VALUABLE_IDS.add("lapis_block");
        VALUABLE_IDS.add("redstone_block");
        VALUABLE_IDS.add("copper_block");
        VALUABLE_IDS.add("bone_block");
        VALUABLE_IDS.add("bamboo_block");
        VALUABLE_IDS.add("dried_kelp_block");
        VALUABLE_IDS.add("deepslate_diamond_ore");
        VALUABLE_IDS.add("deepslate_emerald_ore");
        VALUABLE_IDS.add("deepslate_gold_ore");
        VALUABLE_IDS.add("deepslate_iron_ore");
        VALUABLE_IDS.add("deepslate_lapis_ore");
        VALUABLE_IDS.add("deepslate_redstone_ore");
        VALUABLE_IDS.add("deepslate_copper_ore");
        VALUABLE_IDS.add("deepslate_coal_ore");
        VALUABLE_IDS.add("beacon");
        VALUABLE_IDS.add("dragon_egg");
        VALUABLE_IDS.add("dragon_head");
        VALUABLE_IDS.add("nether_star");
        VALUABLE_IDS.add("elytra");
        VALUABLE_IDS.add("end_portal_frame");
        VALUABLE_IDS.add("breeze_rod");
        VALUABLE_IDS.add("heavy_core");
        VALUABLE_IDS.add("ancient_debris");
        VALUABLE_IDS.add("netherite_ingot");
        VALUABLE_IDS.add("netherite_scrap");
        VALUABLE_IDS.add("conduit");
        VALUABLE_IDS.add("heart_of_the_sea");
        VALUABLE_IDS.add("sponge");
        VALUABLE_IDS.add("wet_sponge");
        VALUABLE_IDS.add("sea_lantern");
        VALUABLE_IDS.add("prismarine");
        VALUABLE_IDS.add("dark_prismarine");
        VALUABLE_IDS.add("prismarine_bricks");
        VALUABLE_IDS.add("nautilus_shell");
        VALUABLE_IDS.add("axolotl_bucket");
        VALUABLE_IDS.add("wither_skeleton_skull");
        VALUABLE_IDS.add("creeper_head");
        VALUABLE_IDS.add("zombie_head");
        VALUABLE_IDS.add("skeleton_skull");
        VALUABLE_IDS.add("piglin_head");
        VALUABLE_IDS.add("player_head");
        VALUABLE_IDS.add("sculk_catalyst");
        VALUABLE_IDS.add("sculk_shrieker");
        VALUABLE_IDS.add("sculk_sensor");
        VALUABLE_IDS.add("calibrated_sculk_sensor");
        VALUABLE_IDS.add("coast_armor_trim_smithing_template");
        VALUABLE_IDS.add("dune_armor_trim_smithing_template");
        VALUABLE_IDS.add("eye_armor_trim_smithing_template");
        VALUABLE_IDS.add("host_armor_trim_smithing_template");
        VALUABLE_IDS.add("raiser_armor_trim_smithing_template");
        VALUABLE_IDS.add("rib_armor_trim_smithing_template");
        VALUABLE_IDS.add("sentry_armor_trim_smithing_template");
        VALUABLE_IDS.add("shaper_armor_trim_smithing_template");
        VALUABLE_IDS.add("silence_armor_trim_smithing_template");
        VALUABLE_IDS.add("snout_armor_trim_smithing_template");
        VALUABLE_IDS.add("spire_armor_trim_smithing_template");
        VALUABLE_IDS.add("tide_armor_trim_smithing_template");
        VALUABLE_IDS.add("vex_armor_trim_smithing_template");
        VALUABLE_IDS.add("ward_armor_trim_smithing_template");
        VALUABLE_IDS.add("wayfinder_armor_trim_smithing_template");
        VALUABLE_IDS.add("wild_armor_trim_smithing_template");
        VALUABLE_IDS.add("netherite_upgrade_smithing_template");
        VALUABLE_IDS.add("bolt_armor_trim_smithing_template");
        VALUABLE_IDS.add("flow_armor_trim_smithing_template");
        VALUABLE_IDS.add("gilded_blackstone");
        VALUABLE_IDS.add("lodestone");
        VALUABLE_IDS.add("budding_amethyst");
        VALUABLE_IDS.add("reinforced_deepslate");
        VALUABLE_IDS.add("blue_ice");
        VALUABLE_IDS.add("enchanted_golden_apple");
        VALUABLE_IDS.add("phantom_membrane");
        VALUABLE_IDS.add("diamond_horse_armor");
        VALUABLE_IDS.add("tipped_arrow");
        VALUABLE_IDS.add("tall_grass");
        VALUABLE_IDS.add("large_fern");
        VALUABLE_IDS.add("brain_coral_block");
        VALUABLE_IDS.add("bubble_coral_block");
        VALUABLE_IDS.add("fire_coral_block");
        VALUABLE_IDS.add("horn_coral_block");
        VALUABLE_IDS.add("tube_coral_block");
        VALUABLE_IDS.add("dead_brain_coral_block");
        VALUABLE_IDS.add("dead_bubble_coral_block");
        VALUABLE_IDS.add("dead_fire_coral_block");
        VALUABLE_IDS.add("dead_horn_coral_block");
        VALUABLE_IDS.add("dead_tube_coral_block");
    }
}
