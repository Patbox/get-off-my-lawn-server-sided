package draylar.goml.api;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;


// Original implementation https://github.com/NucleoidMC/plasmid/blob/1.16/src/main/java/xyz/nucleoid/plasmid/map/workspace/editor/ParticleOutlineRenderer.java
public class WorldParticleUtils {
    public static void render(ServerPlayerEntity player, BlockPos min, BlockPos max, ParticleEffect effect) {
        Edge[] edges = edges(min, max);

        int maxInterval = 5;
        int maxCount = 40;

        for (Edge edge : edges) {
            int length = edge.length();

            double interval = 1;
            if (length > 0) {
                interval = MathHelper.clamp(length / Math.min(maxCount, length), 1, maxInterval);
            }

            double steps = (length + interval - 1) / interval;
            for (double i = 0; i < steps; i++) {
                double m = (i * interval) / length;
                spawnParticleIfVisible(
                        player, effect,
                        edge.projX(m) + 0.5, edge.projY(m) + 0.5, edge.projZ(m) + 0.5
                );
            }
        }
    }

    private static void spawnParticleIfVisible(ServerPlayerEntity player, ParticleEffect effect, double x, double y, double z) {
        ServerWorld world = player.getServerWorld();

        Vec3d delta = player.getPos().subtract(x, y, z);
        double length2 = delta.lengthSquared();
        if (length2 > 512 * 512) {
            return;
        }

        /*Vec3d rotation = player.getRotationVec(1.0F);
        double dot = (delta.multiply(1.0 / Math.sqrt(length2))).dotProduct(rotation);
        if (dot > 0.0) {
            return;
        }*/

        world.spawnParticles(
                player, effect, true, true,
                x, y, z,
                1,
                0.0, 0.0, 0.0,
                0.0
        );
    }

    private static Edge[] edges(BlockPos min, BlockPos max) {
        var list = new ArrayList<Edge>();
        int minX = min.getX();
        int minY = min.getY();
        int minZ = min.getZ();
        int maxX = max.getX();
        int maxY = max.getY();
        int maxZ = max.getZ();

        // edges


        list.add(new Edge(minX, minY, minZ, minX, maxY, minZ));
        list.add(new Edge(maxX, minY, minZ, maxX, maxY, minZ));

        list.add(new Edge(minX, minY, maxZ, minX, maxY, maxZ));
        list.add(new Edge(maxX, minY, maxZ, maxX, maxY, maxZ));

        list.add(new Edge(minX, minY, minZ, maxX, minY, minZ));
        list.add(new Edge(minX, minY, maxZ, maxX, minY, maxZ));
        list.add(new Edge(maxX, minY, minZ, maxX, minY, maxZ));
        list.add(new Edge(minX, minY, minZ, minX, minY, maxZ));

        list.add(new Edge(minX, maxY, minZ, minX, maxY, maxZ));
        list.add(new Edge(maxX, maxY, minZ, maxX, maxY, maxZ));
        list.add(new Edge(minX, maxY, minZ, maxX, maxY, minZ));
        list.add(new Edge(minX, maxY, maxZ, maxX, maxY, maxZ));

        var height = (max.getY() - min.getY());

        var count = MathHelper.ceil(height / 64d);

        var delta = height / count;

        for (int i = 1; i < count; i++) {
            list.add(new Edge(minX, minY + i * delta, minZ, maxX, minY + i * delta, minZ));
            list.add(new Edge(minX, minY + i * delta, maxZ, maxX, minY + i * delta, maxZ));
            list.add(new Edge(maxX, minY + i * delta, minZ, maxX, minY + i * delta, maxZ));
            list.add(new Edge(minX, minY + i * delta, minZ, minX, minY + i * delta, maxZ));
        }

        return list.toArray(new Edge[0]);
    }

    private record Edge(int startX, int startY, int startZ, int endX, int endY, int endZ) {

        double projX(double m) {
            return this.startX + (this.endX - this.startX) * m;
        }

        double projY(double m) {
            return this.startY + (this.endY - this.startY) * m;
        }

        double projZ(double m) {
            return this.startZ + (this.endZ - this.startZ) * m;
        }

        int length() {
            int dx = this.endX - this.startX;
            int dy = this.endY - this.startY;
            int dz = this.endZ - this.startZ;
            return MathHelper.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz));
        }
    }
}