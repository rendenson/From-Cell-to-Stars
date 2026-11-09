package com.celltostars.network;

import com.celltostars.CellToStars;
import com.celltostars.data.Diet;
import com.celltostars.data.Part;
import com.celltostars.data.EvoData;
import com.celltostars.logic.EvoCycle;
import com.celltostars.logic.EvoLogic;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Messages between the game server and the player's client. */
public final class EvoNetwork {
    public static final int EDITOR_CREATE = 0; // brand new world: create your cell
    public static final int EDITOR_MATE = 1;   // after finding a mate
    public static final int EDITOR_RESUME = 2; // after re-joining the world

    // ---------- server -> client ----------

    /** "Here is your current EvoData". */
    public record SyncPayload(EvoData data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SyncPayload> TYPE = new CustomPacketPayload.Type<>(CellToStars.id("sync"));
        public static final StreamCodec<ByteBuf, SyncPayload> CODEC = EvoData.STREAM_CODEC.map(SyncPayload::new, SyncPayload::data);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** "You are in the egg now: show the editor". */
    public record OpenEditorPayload(int mode) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<OpenEditorPayload> TYPE = new CustomPacketPayload.Type<>(CellToStars.id("open_editor"));
        public static final StreamCodec<ByteBuf, OpenEditorPayload> CODEC = ByteBufCodecs.VAR_INT.map(OpenEditorPayload::new, OpenEditorPayload::mode);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** "You found your mate: play the love scene". */
    public record LovePayload() implements CustomPacketPayload {
        public static final LovePayload INSTANCE = new LovePayload();
        public static final CustomPacketPayload.Type<LovePayload> TYPE = new CustomPacketPayload.Type<>(CellToStars.id("love"));
        public static final StreamCodec<ByteBuf, LovePayload> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** "Play the egg-cracking scene" (after respawning; after the editor the client starts it by itself). */
    public record HatchScenePayload() implements CustomPacketPayload {
        public static final HatchScenePayload INSTANCE = new HatchScenePayload();
        public static final CustomPacketPayload.Type<HatchScenePayload> TYPE = new CustomPacketPayload.Type<>(CellToStars.id("hatch_scene"));
        public static final StreamCodec<ByteBuf, HatchScenePayload> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // ---------- client -> server ----------

    /** "Buy one more of this part" (buy = true) or "sell one back" (buy = false). */
    public record PartPayload(String part, boolean buy) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PartPayload> TYPE = new CustomPacketPayload.Type<>(CellToStars.id("part"));
        public static final StreamCodec<ByteBuf, PartPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, PartPayload::part,
                ByteBufCodecs.BOOL, PartPayload::buy,
                PartPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** "Switch my diet to this". */
    public record SetDietPayload(String diet) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetDietPayload> TYPE = new CustomPacketPayload.Type<>(CellToStars.id("set_diet"));
        public static final StreamCodec<ByteBuf, SetDietPayload> CODEC = ByteBufCodecs.STRING_UTF8.map(SetDietPayload::new, SetDietPayload::diet);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** "Paint my cell like this". */
    public record SetLookPayload(int primary, int secondary, String pattern) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetLookPayload> TYPE = new CustomPacketPayload.Type<>(CellToStars.id("set_look"));
        public static final StreamCodec<ByteBuf, SetLookPayload> CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, SetLookPayload::primary,
                ByteBufCodecs.INT, SetLookPayload::secondary,
                ByteBufCodecs.STRING_UTF8, SetLookPayload::pattern,
                SetLookPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** "Done in the editor, hatch me (with this name)". */
    public record HatchPayload(String name) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HatchPayload> TYPE = new CustomPacketPayload.Type<>(CellToStars.id("hatch"));
        public static final StreamCodec<ByteBuf, HatchPayload> CODEC = ByteBufCodecs.STRING_UTF8.map(HatchPayload::new, HatchPayload::name);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** "Call a mate for me". */
    public record CallMatePayload() implements CustomPacketPayload {
        public static final CallMatePayload INSTANCE = new CallMatePayload();
        public static final CustomPacketPayload.Type<CallMatePayload> TYPE = new CustomPacketPayload.Type<>(CellToStars.id("call_mate"));
        public static final StreamCodec<ByteBuf, CallMatePayload> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** "I want to evolve" (from the editor, with the name typed there). */
    public record EvolvePayload(String name) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<EvolvePayload> TYPE = new CustomPacketPayload.Type<>(CellToStars.id("evolve"));
        public static final StreamCodec<ByteBuf, EvolvePayload> CODEC = ByteBufCodecs.STRING_UTF8.map(EvolvePayload::new, EvolvePayload::name);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private EvoNetwork() {
    }

    public static void init() {
        PayloadTypeRegistry.playS2C().register(SyncPayload.TYPE, SyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenEditorPayload.TYPE, OpenEditorPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LovePayload.TYPE, LovePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(HatchScenePayload.TYPE, HatchScenePayload.CODEC);

        PayloadTypeRegistry.playC2S().register(PartPayload.TYPE, PartPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetDietPayload.TYPE, SetDietPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetLookPayload.TYPE, SetLookPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HatchPayload.TYPE, HatchPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CallMatePayload.TYPE, CallMatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(EvolvePayload.TYPE, EvolvePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PartPayload.TYPE, (payload, context) -> {
            Part part = Part.byId(payload.part());
            if (part == null) return;
            if (payload.buy()) EvoCycle.buyPart(context.player(), part);
            else EvoCycle.sellPart(context.player(), part);
        });
        ServerPlayNetworking.registerGlobalReceiver(SetDietPayload.TYPE, (payload, context) -> {
            Diet diet = Diet.byName(payload.diet());
            if (diet != null) EvoCycle.setDiet(context.player(), diet);
        });
        ServerPlayNetworking.registerGlobalReceiver(SetLookPayload.TYPE, (payload, context) ->
                EvoCycle.setLook(context.player(), payload.primary(), payload.secondary(), payload.pattern()));
        ServerPlayNetworking.registerGlobalReceiver(HatchPayload.TYPE, (payload, context) ->
                EvoCycle.hatch(context.player(), payload.name()));
        ServerPlayNetworking.registerGlobalReceiver(CallMatePayload.TYPE, (payload, context) ->
                EvoCycle.callMate(context.player()));
        ServerPlayNetworking.registerGlobalReceiver(EvolvePayload.TYPE, (payload, context) -> {
            if (EvoCycle.isInEgg(context.player())) EvoCycle.rename(context.player(), payload.name());
            EvoLogic.evolve(context.player(), false);
        });
    }
}
