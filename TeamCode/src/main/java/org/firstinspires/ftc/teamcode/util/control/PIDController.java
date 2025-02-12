/*
 * Filename: PIDController.java
 * Author: Cole Savage
 * Team Name: Level Up
 * Date: 7/17/19 - 8/9/19
 */

package org.firstinspires.ftc.teamcode.util.control;

import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.util.functional_interfaces.BiFunction;

/**
 * A PID controller class with multiple modes.
 */
public class PIDController {

    //Function used to calculate error value.
    private BiFunction<Double,Double,Double> errorFunction;

    //PID(f) coefficients and component values.
    private double kp,ki,kd,kf,P,I,D,F;
    
    //PID target value.
    private double setpoint;
    
    //Storage variables for past values.
    private double lastState, lastOutput;

    //Ranges for clamping components of the PID controller.
    private double iClampLower,clampLower,iClampUpper,clampUpper,pClampLower, pClampUpper;

    //The system time in milliseconds that the last update to the PID controller occurred at.
    private long lastUpdate;

    //A boolean specifying if the controller is currently active.
    private boolean active;

    /**
     * Specifies the type of control system we want active.
     */
    private Type type;
    public enum Type {
        STANDARD, FEED_FORWARD, P_ON_M
    }

    /**
     * Constructor for PID controller with specified coefficients.
     *
     * @param kp - Proportional control coefficient
     * @param ki - Integral control coefficient
     * @param kd - Derivative control coefficient
     */
    public PIDController(double kp, double ki, double kd) {
        this(kp,ki,kd,(Double target, Double current) -> (target - current),Type.STANDARD);
    }

    /**
     * Constructor for specified error function in addition to specified coefficients.
     *
     * @param kp - Proportional control coefficient
     * @param ki - Integral control coefficient
     * @param kd - Derivative control coefficient
     * @param errorFunction - Specified error function to use for control
     */
    public PIDController(double kp, double ki, double kd, BiFunction<Double,Double,Double> errorFunction) {
        this(kp,ki,kd,errorFunction,Type.STANDARD);
    }

    /**
     * Constructor for specified type of control in addition to specified coefficients.
     *
     * @param kp - Proportional control coefficient.
     * @param ki - Integral control coefficient.
     * @param kd - Derivative control coefficient.
     * @param type - Type of control system to use.
     */
    public PIDController(double kp, double ki, double kd, Type type) {
        this(kp,ki,kd,(Double target, Double current) -> (target - current),type);
    }
    
    /**
     * Constructor for specified error function and type of control in addition to specified coefficients.
     *
     * @param kp - Proportional control coefficient
     * @param ki - Integral control coefficient
     * @param kd - Derivative control coefficient
     * @param errorFunction - Specified error function to use for control
     * @param type - Type of control system to use
     */
    public PIDController(double kp, double ki, double kd, BiFunction<Double,Double,Double> errorFunction, Type type) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.kf = 0;
        this.errorFunction = errorFunction;
        this.type = type;
    }

    /**
     * Constructor for PIDF controller with custom error function.
     *
     * @param kp - Proportional control coefficient.
     * @param ki - Integral control coefficient.
     * @param kd - Derivative control coefficient.
     * @param kf - Feedforward control coefficient.
     * @param errorFunction - Specified error function to use for control.
     */
    public PIDController(double kp, double ki, double kd, double kf, BiFunction<Double,Double,Double> errorFunction) {
        this(kp,ki,kd,kf,errorFunction,Type.FEED_FORWARD);
    }

    /**
     * Constructor for PID(F) controller with adjustable/overrideable type and default error function.
     *
     * @param kp - Proportional control coefficient.
     * @param ki - Integral control coefficient.
     * @param kd - Derivative control coefficient.
     * @param kf - Feedforward control coefficient.
     * @param type - The type of the PID(F) controller.
     */
    public PIDController(double kp, double ki, double kd, double kf, Type type) {
        this(kp,ki,kd,kf,(Double target, Double current) -> (target - current),type);
    }
    
    /**
     * Constructor for PIDF controller with default error function.
     *
     * @param kp - Proportional control coefficient.
     * @param ki - Integral control coefficient.
     * @param kd - Derivative control coefficient.
     * @param kf - Feedforward control coefficient.
     */
    public PIDController(double kp, double ki, double kd, double kf) {
        this(kp,ki,kd,kf,(Double target, Double current) -> (target - current),Type.FEED_FORWARD);
    }
    
    /**
     * Constructor for PID(F) controller with adjustable/overrideable type and custom error function.
     *
     * @param kp - Proportional control coefficient.
     * @param ki - Integral control coefficient.
     * @param kd - Derivative control coefficient.
     * @param kf - Feedforward control coefficient.
     * @param errorFunction - Specified error function to use for control.
     * @param type - The type of the PID(F) controller.
     */
    public PIDController(double kp, double ki, double kd, double kf, BiFunction<Double,Double,Double> errorFunction, Type type) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.kf = kf;
        this.errorFunction = errorFunction;
        this.type = type;
    }

    /**
     * Initializes the control system with a target and initial state.
     *
     * @param setpoint - Target value of anything you want to control
     * @param initialState - Initial control mode of PID controller
     */
    public void init(double setpoint, double initialState) {
        this.setpoint = setpoint;
        this.lastState = initialState;
        lastUpdate = 0;
        iClampLower = -Double.MAX_VALUE;
        clampLower = -Double.MAX_VALUE;
        pClampLower = -Double.MAX_VALUE;
        iClampUpper = Double.MAX_VALUE;
        clampUpper = Double.MAX_VALUE;
        pClampUpper = Double.MAX_VALUE;
        P = 0;
        I = 0;
        D = 0;
        F = 0;
        active = true;
    }

    /**
     * Enables the PID controller.
     *
     * @param current - current angle position or state
     */
    public void enable(double current) {
        I = 0;
        lastState = current;
        lastUpdate = System.currentTimeMillis();
        active = true;
    }

    /**
     * Disables the PID controller.
     */
    public void disable() {
        lastOutput = Range.clip(P + I + D,clampLower,clampUpper);
        active = false;
    }

    /**
     * Sets the integral clamp values.
     *
     * @param lower - New lower bound
     * @param upper - New upper bound
     */
    public void setIClamp(double lower, double upper) {
        iClampLower = lower;
        iClampUpper = upper;
    }

    /**
     * Sets the output clamp values.
     *
     * @param lower - New lower bound
     * @param upper - New upper bound
     */
    public void setOutputClamp(double lower, double upper) {
        clampLower = lower;
        clampUpper = upper;
    }

    /**
     * Sets the proportional on measurement clamp values.
     *
     * @param lower - New lower bound
     * @param upper - New upper bound
     */
    public void setPonMClamp(double lower, double upper) {
        pClampLower = lower;
        pClampUpper= upper;
    }

    /**
     * Sets the target value of the controller.
     * 
     * @param setpoint - New target for the controller
     */
    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
    }

    public double getSetpoint() {
        return setpoint;
    }

    /**
     * Sets the tuning values for the PID controller.
     *
     * @param kp - New proportional coefficient
     * @param ki - New integral coefficient
     * @param kd - New derivative coefficient
     */
    public void setTunings(double kp, double ki, double kd) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
    }

    /**
     * Gets the correction value based on the controller calculations.
     *
     * @param current - Current measured state of whatever you're trying to control
     * @return result - Correction based on PID controller calculations.
     */
    public double getCorrection(double current) {

        if(!active) { // Controller disabled
            return 0;
        }

        double dT = lastUpdate == 0 ? 0 : (System.currentTimeMillis() - lastUpdate) / 1000.0; //because I like seconds
        double error = getError(current);

        switch(type) {
            case FEED_FORWARD:
                P = kp * error;
                I = Range.clip(I + ki * error * dT, iClampLower, iClampUpper);
                D = dT <= 0.0001 ? 0 : -kd * (current - lastState) / dT;
                F = kf*setpoint;
                lastState = current;
                lastUpdate = System.currentTimeMillis();

                return Range.clip(P + I + D + F,clampLower,clampUpper);
            case P_ON_M:
                P = Range.clip(P-kp*(current - lastState),pClampLower,pClampUpper);
                I = Range.clip(I + ki * error * dT, iClampLower, iClampUpper);
                D = dT <= 0.0001 ? 0 : -kd * (current - lastState) / dT;
                lastState = current;
                lastUpdate = System.currentTimeMillis();

                return Range.clip(P + I + D,clampLower,clampUpper);
            default:
                P = kp * error;
                I = Range.clip(I + ki * error * dT, iClampLower, iClampUpper);
                D = dT <= 0.0001 ? 0 : -kd * (current - lastState) / dT;
                lastState = current;
                lastUpdate = System.currentTimeMillis();

                return Range.clip(P + I + D,clampLower,clampUpper);
        }
    }
    
    public double getError(double current) {
        return errorFunction.apply(setpoint, current);
    }
}
