package monoton.utils.other;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class RandUtils {
    private final SecureRandom baseRandom = new SecureRandom();
    private static double lastValue = Double.NaN;
    private static int counter = 0;

    private static double lastVelocity = 0.0;
    private static int fatigueCounter = 0;


    private static double constrain(double value, double min, double max) {
        if (value >= min && value <= max) return value;

        double scaled = Math.abs(value) % (max - min);
        return scaled + min - ((int)(scaled / (max - min)) * (max - min));
    }

    public enum PatternMode {
        LINEAR_SMOOTH,
        GAUSSIAN_CENTERED,
        EDGE_BIASED,
        STEPPED,
        CHAOTIC_SMOOTH,
        HUMAN_TREMOR,
        SACCADIC_MOVEMENT,
        MICRO_CORRECTIONS,
        INERTIA_SMOOTH,
        FATIGUE_DRIFT,
        TREMOR_PREDICT,
        FATIGURE_ADVANCE,
        INTERIA_DECREATE
    }

    public static double LegitDouble(double min, double max, PatternMode mode) {
        if (min >= max) return min;

        double value;
        switch (mode) {
            case LINEAR_SMOOTH:
                value = linearSmooth(min, max);
                break;
            case GAUSSIAN_CENTERED:
                value = gaussianCentered(min, max);
                break;
            case EDGE_BIASED:
                value = edgeBiased(min, max);
                break;
            case STEPPED:
                value = stepped(min, max);
                break;
            case CHAOTIC_SMOOTH:
                value = chaoticSmooth(min, max);
                break;
            case HUMAN_TREMOR:
                value = humanTremor(min, max);
                break;
            case SACCADIC_MOVEMENT:
                value = saccadicMovement(min, max);
                break;
            case MICRO_CORRECTIONS:
                value =  microCorrections(min, max);
                break;
            case INERTIA_SMOOTH:
                value = inertiaSmooth(min, max);
                break;
            case FATIGUE_DRIFT:
                value = fatigueDrift(min, max);
                break;
            case TREMOR_PREDICT:
                value = Pred_Tremor(min, max);
                break;
            case INTERIA_DECREATE:
                value = interia2(min, max);
                break;
            case FATIGURE_ADVANCE:
                value = ADVfatigueDrift(min, max);
                break;
            default:
                value = baseRandom.nextDouble() * (max - min) + min;
        }

        lastValue = value;
        counter++;
        return value;
    }

    public static float LegitFloat(float min, float max, PatternMode mode) {
        return (float) LegitDouble(min, max, mode);
    }

    private static double Pred_Tremor(double min, double max) {
        double baseValue = Double.isNaN(lastValue)
                ? (min + max) / 2.0
                : lastValue;

        double timeFactor = counter * 0.3;
        double tremorX = Math.sin(timeFactor * 25.3) * 0.4;
        double tremorY = Math.cos(timeFactor * 22.7) * 0.3;
        double tremorZ = Math.sin(timeFactor * 18.9) * 0.3;

        double tremor = (tremorX + tremorY + tremorZ) / 3.0;
        tremor *= 0.03 * (max - min);

        if(counter % 15 == 0) {
            tremor += baseRandom.nextGaussian() * 0.02 * (max - min);
        }

        return constrain(baseValue + tremor, min, max);
    }

    private static double linearSmooth(double min, double max) {
        if (Double.isNaN(lastValue)) {
            return baseRandom.nextDouble() * (max - min) + min;
        }

        double range = max - min;
        double step = range * 0.05 * (baseRandom.nextGaussian() + 0.3);
        double newValue = lastValue + step;

        if (newValue < min || newValue > max) {
            double target = min + range * (0.2 + 0.6 * baseRandom.nextDouble());
            return target;
        }

        return newValue;
    }

    private static double gaussianCentered(double min, double max) {
        double center = (min + max) / 2.0;
        double range = max - min;

        double value = center + baseRandom.nextGaussian() * (range / 6.0);

        if (counter % 5 == 0) {
            double offset = (value - center) * 0.7;
            value = center + offset;
        }

        return Math.max(min, Math.min(max, value));
    }

    private static double edgeBiased(double min, double max) {
        double targetEdge = baseRandom.nextBoolean() ? min : max;

        double biasStrength = 0.7 + baseRandom.nextDouble() * 0.25;
        double value;

        if (targetEdge == min) {
            value = min + Math.abs(baseRandom.nextGaussian()) * (max - min) * 0.3;
        } else {
            value = max - Math.abs(baseRandom.nextGaussian()) * (max - min) * 0.3;
        }

        if (counter % 7 == 0) {
            double center = (min + max) / 2.0;
            value = center + (value - center) * 0.4;
        }

        return value;
    }

    private static double stepped(double min, double max) {
        double range = max - min;
        int steps = 5 + baseRandom.nextInt(6); // 5-10 ступеней

        int currentStep = (int) (steps * baseRandom.nextDouble());

        if (counter % 4 == 0) {
            currentStep = (currentStep + 1 + baseRandom.nextInt(2)) % steps;
        }

        double stepSize = range / steps;
        double stepMin = min + currentStep * stepSize;
        double stepMax = stepMin + stepSize;

        return stepMin + baseRandom.nextDouble() * (stepMax - stepMin);
    }

    private static double chaoticSmooth(double min, double max) {
        if (Double.isNaN(lastValue)) {
            return baseRandom.nextDouble() * (max - min) + min;
        }

        double range = max - min;
        double chaosFactor = 0.2 + baseRandom.nextDouble() * 0.5;
        double direction = baseRandom.nextBoolean() ? 1 : -1;

        double delta = range * chaosFactor * direction * Math.sin(counter * 0.5);
        double newValue = lastValue + delta;

        if (newValue < min || newValue > max) {
            double anchor = min + range * (0.1 + 0.8 * baseRandom.nextDouble());
            return anchor;
        }

        return newValue;
    }

    private static double humanTremor(double min, double max) {
        double baseValue = Double.isNaN(lastValue)
                ? (min + max) / 2.0
                : lastValue;

        double tremorIntensity = 0.02 + Math.sin(counter * 0.3) * 0.01;
        double tremor = Math.sin(counter * 12.7) * tremorIntensity * (max - min);

        if(counter % 15 == 0) {
            tremor += baseRandom.nextGaussian() * 0.04 * (max - min);
        }

        return constrain(baseValue + tremor, min, max);
    }

    private static double saccadicMovement(double min, double max) {
        if(Double.isNaN(lastValue)) {
            return min + baseRandom.nextDouble() * (max - min);
        }

        int fixationDuration = 10 + baseRandom.nextInt(15);

        if(counter % fixationDuration == 0) {
            double jumpSize = (max - min) * (0.1 + baseRandom.nextDouble() * 0.15);
            int direction = baseRandom.nextBoolean() ? 1 : -1;
            return constrain(lastValue + jumpSize * direction, min, max);
        }

        return lastValue + baseRandom.nextGaussian() * 0.015 * (max - min);
    }

    private static double microCorrections(double min, double max) {
        double target = Double.isNaN(lastValue)
                ? min + (max - min) * (0.3 + baseRandom.nextDouble() * 0.4)
                : lastValue;

        if(counter % (8 + baseRandom.nextInt(5)) == 0) {
            double overshoot = (max - min) * 0.08 * (baseRandom.nextGaussian() + 0.5);
            target += baseRandom.nextBoolean() ? overshoot : -overshoot;
        }

        double correction = (target - lastValue) * 0.15 + baseRandom.nextGaussian() * 0.01;
        return constrain(lastValue + correction, min, max);
    }

    private static double inertiaSmooth(double min, double max) {
        if(Double.isNaN(lastValue)) {
            return min + baseRandom.nextDouble() * (max - min);
        }

        double velocity = Double.isNaN(lastVelocity)
                ? (max - min) * 0.005 * (baseRandom.nextGaussian() + 0.3)
                : lastVelocity * 0.85;

        if(baseRandom.nextDouble() < 0.07) {
            velocity *= -0.5 + baseRandom.nextDouble();
        }

        double newValue = lastValue + velocity;
        lastVelocity = velocity;

        if(newValue < min || newValue > max) {
            lastVelocity *= -0.7;
            return constrain(lastValue + lastVelocity, min, max);
        }

        return newValue;
    }

    private static double interia2(double min, double max){
        if(Double.isNaN(lastValue)) {
            lastVelocity = (baseRandom.nextGaussian() * 0.5 + 0.3) * (max - min) * 0.01;
            return min + baseRandom.nextDouble() * (max - min);
        }

        lastVelocity *= 0.82;

        if(baseRandom.nextDouble() < 0.12) {
            lastVelocity += (baseRandom.nextGaussian() * 0.7) * (max - min) * 0.005;
        }

        double newValue = lastValue + lastVelocity;

        if(newValue < min || newValue > max) {
            lastVelocity *= -0.65;
            return constrain(lastValue + lastVelocity, min, max);
        }

        return newValue;
    }

    private static double fatigueDrift(double min, double max) {
        if(Double.isNaN(lastValue)) {
            return min + (max - min) * 0.5;
        }

        double fatigueFactor = Math.min(1.0, counter / 300.0);

        double drift = (baseRandom.nextDouble() - 0.4) * fatigueFactor * 0.08 * (max - min);

        if(counter % (30 - (int)(fatigueFactor * 15)) == 0) {
            double correction = ((min + max)/2 - lastValue) * (0.3 + baseRandom.nextDouble() * 0.4);
            drift += correction;
        }

        return constrain(lastValue + drift, min, max);
    }

    private static double ADVfatigueDrift(double min, double max) {
        if(Double.isNaN(lastValue)) {
            fatigueCounter = 0;
            return min + (max - min) * 0.5;
        }

        fatigueCounter++;

        double fatigueFactor = Math.min(1.0, fatigueCounter / 250.0);

        double drift = (baseRandom.nextDouble() - 0.42) * fatigueFactor * 0.06 * (max - min);

        int correctionInterval = 35 - (int)(fatigueFactor * 20);
        if(correctionInterval < 8) correctionInterval = 8;

        if(fatigueCounter % correctionInterval == 0) {
            double correction = ((min + max)/2 - lastValue) * (0.25 + baseRandom.nextDouble() * 0.35);
            drift += correction;

            fatigueCounter = (int)(fatigueCounter * 0.6);
        }

        return constrain(lastValue + drift, min, max);
    }
}