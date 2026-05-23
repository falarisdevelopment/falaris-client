package dev.falaris.client.module.modules.player;

import dev.falaris.client.setting.BooleanSetting;
import dev.falaris.client.setting.IntegerSetting;
import dev.falaris.client.setting.ModeSetting;
import dev.falaris.client.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.village.TradeOffer;

import java.util.Locale;
import java.util.stream.Collectors;

public final class AutoLibrarian extends PlayerModule {
    private final TextSetting enchantQuery = setting(new TextSetting("Enchant Query", "Case-insensitive enchant text to look for.", "mending"));
    private final IntegerSetting maxEmeralds = setting(new IntegerSetting("Max Emeralds", "Maximum emerald price accepted.", 24, 1, 64));
    private final IntegerSetting minLevel = setting(new IntegerSetting("Min Level", "Minimum trade index to scan from.", 1, 1, 5));
    private final IntegerSetting delay = setting(new IntegerSetting("Delay", "Ticks between librarian checks.", 10, 1, 100));
    private final IntegerSetting jitter = setting(new IntegerSetting("Jitter", "Random extra ticks between checks.", 5, 0, 100));
    private final ModeSetting mode = setting(new ModeSetting("Mode", "What to do when a matching trade is found.", "Notify", "Notify", "Select", "Close On Miss"));
    private final BooleanSetting enchantedBooksOnly = setting(new BooleanSetting("Books Only", "Only accept enchanted book trades.", true));

    private String lastMatch = "";

    public AutoLibrarian() {
        super("AutoLibrarian", "Scans librarian trades for desired enchanted books.");
    }

    @Override
    protected void onPlayerTick(MinecraftClient client) {
        if (!(client.currentScreen instanceof MerchantScreen) || !(client.player.currentScreenHandler instanceof MerchantScreenHandler handler)) {
            return;
        }
        if (!ready(delay.get(), jitter.get())) {
            return;
        }

        String query = enchantQuery.get().toLowerCase(Locale.ROOT).trim();
        for (int index = Math.max(0, minLevel.get() - 1); index < handler.getRecipes().size(); index++) {
            TradeOffer offer = handler.getRecipes().get(index);
            ItemStack sell = offer.getSellItem();
            int emeralds = emeraldCost(offer);
            String text = searchableText(client, sell);
            boolean book = sell.isOf(Items.ENCHANTED_BOOK);

            if (enchantedBooksOnly.enabled() && !book) {
                continue;
            }
            if (emeralds > maxEmeralds.get()) {
                continue;
            }
            if (!query.isEmpty() && !text.contains(query)) {
                continue;
            }

            handler.setRecipeIndex(index);
            lastMatch = "Trade " + (index + 1) + ": " + sell.getName().getString() + " for " + emeralds + " emeralds";
            if (mode.is("Notify") || mode.is("Select")) {
                client.player.sendMessage(net.minecraft.text.Text.literal("[Falaris] Librarian match: " + lastMatch), false);
            }
            return;
        }

        lastMatch = "";
        if (mode.is("Close On Miss")) {
            client.player.closeHandledScreen();
        }
    }

    private int emeraldCost(TradeOffer offer) {
        int cost = 0;
        if (offer.getDisplayedFirstBuyItem().isOf(Items.EMERALD)) {
            cost += offer.getDisplayedFirstBuyItem().getCount();
        }
        if (offer.getDisplayedSecondBuyItem().isOf(Items.EMERALD)) {
            cost += offer.getDisplayedSecondBuyItem().getCount();
        }
        return cost == 0 ? 64 : cost;
    }

    private String searchableText(MinecraftClient client, ItemStack stack) {
        String tooltip = stack.getTooltip(Item.TooltipContext.create(client.world), client.player, TooltipType.BASIC)
                .stream()
                .map(Text::getString)
                .collect(Collectors.joining(" "));
        return (stack.getName().getString() + " " + tooltip).toLowerCase(Locale.ROOT);
    }

    public String getLastMatch() {
        return lastMatch;
    }

    private static final class TextSetting extends Setting<String> {
        private TextSetting(String name, String description, String defaultValue) {
            super(name, description, defaultValue);
        }
    }
}
