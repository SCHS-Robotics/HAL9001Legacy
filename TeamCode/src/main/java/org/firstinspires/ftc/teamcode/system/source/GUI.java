/*
 * Filename: GUI.java
 * Author: Cole Savage and Dylan Zueck
 * Team Name: Level Up, Crow Force
 * Date: 7/20/19
 */

package org.firstinspires.ftc.teamcode.system.source;

import org.firstinspires.ftc.teamcode.util.exceptions.NotBooleanInputException;
import org.firstinspires.ftc.teamcode.util.gui_lib.GuiLine;
import org.firstinspires.ftc.teamcode.util.misc.Button;
import org.firstinspires.ftc.teamcode.util.misc.CustomizableGamepad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Gui class for drawing and handling menus. Think of it like the robot.java class but for graphics.
 */
public class GUI {

    private Menu activeMenu;
    private Map<String,Menu> menus;
    public Robot robot;
    private Cursor cursor;

    private int cursorBlinkState, activeMenuIdx;
    private double lastBlinkTimeMs;

    private ArrayList<String> menuKeys;

    private boolean flag;

    private CustomizableGamepad inputs = new CustomizableGamepad(robot);

    private static final String CYCLE_MENUS = "CycleMenus";

    /**
     * Ctor for GUI.
     *
     * @param robot - The robot using the instance of GUI.
     * @param cursor - The cursor object all menus handled by the GUI will use.
     * @param flipMenu - The button used to cycle between multiple stored menus.
     *
     * @throws NotBooleanInputException - Throws an exception if button does not return boolean values.
     */
    protected GUI(Robot robot, Cursor cursor, Button flipMenu) {
        this.cursor = cursor;
        this.robot = robot;
        this.menus = new HashMap<>();

        menuKeys = new ArrayList<>();
        
        if(flipMenu.isBoolean) {
            this.inputs.addButton(CYCLE_MENUS, flipMenu);
        }
        else {
            throw new NotBooleanInputException("A non-boolean input was passed to the controller as a boolean input");
        }

        cursorBlinkState = 0;
        lastBlinkTimeMs = System.currentTimeMillis();
        flag = true;
        activeMenuIdx = 0;
    }

    /**
     * Runs the init() function for every menu contained in the GUI.
     */
    protected void start(){
        for(Menu m : menus.values()) {
            m.init(cursor);
        }
    }

    /**
     * Draws the current active menu to the screen.
     */
    protected void drawCurrentMenu(){
        cursor.update();
        if(inputs.getBooleanInput(CYCLE_MENUS) && flag){
            activeMenuIdx++;
            activeMenuIdx = activeMenuIdx % menuKeys.size();
            setActiveMenu(menuKeys.get(activeMenuIdx));
        }
        else if(!inputs.getBooleanInput(CYCLE_MENUS) && !flag) {
            flag = true;
        }
        activeMenu.render();
        robot.telemetry.update();
    }

    /**
     * Runs the stop function for every menu contained in the GUI.
     */
    protected void stop() {
        for (Menu m : menus.values()) {
            m.stop();
        }
        clearScreen();
    }

    /**
     * Adds a menu to the GUI.
     *
     * @param name - The menu object to be added.
     * @param menu - The name of the menu.
     */
    public void addMenu(String name, Menu menu){
        menus.put(name, menu);
        menuKeys.add(name);
    }

    /**
     * Removes a menu from the GUI.
     *
     * @param name - The name of the menu to be removed.
     */
    public void removeMenu(String name) {
        if(menuKeys.indexOf(name) > activeMenuIdx && activeMenuIdx != menuKeys.size()-1){
            activeMenuIdx--;
        }
        menuKeys.remove(name);
        menus.remove(name);
        activeMenuIdx = activeMenuIdx % menuKeys.size();
        setActiveMenu(menuKeys.get(activeMenuIdx));
    }

    /**
     * Sets the active menu.
     *
     * @param menuName - The name of the menu to be set as the active menu.
     */
    protected void setActiveMenu(String menuName){
        this.activeMenu = menus.get(menuName);
        menus.get(menuName).open();
        cursor.setCurrentMenu(menus.get(menuName));
    }

    //TODO, make this not burn our eyes

    /**
     * Causes the cursor to blink on a specified line.
     *
     * @param line - The line object where the cursor is currently located.
     */
    private void blinkCursor(GuiLine line) {
        ArrayList<Character> cursorLineChars = new ArrayList<>();
        char[] chars = line.selectionZoneText.toCharArray();
        for(int i = 0; i < chars.length; i++) {
            if(i == cursor.getX()) {
                switch (cursorBlinkState) {
                    case 0:
                        cursorLineChars.add(cursor.getCursorIcon());
                        break;
                    case 1:
                    case 3:
                        cursorLineChars.add(' ');
                        break;
                    default:
                        cursorLineChars.add(chars[i]);
                        break;
                }
            }
            else {
                cursorLineChars.add(chars[i]);
            }
        }
        StringBuilder sb = new StringBuilder();
        for(Character ch: cursorLineChars){
            sb.append(ch);
        }
        String selectionZoneText = sb.toString();
        robot.telemetry.addLine(line.getLineTextReplaceSelectionZoneText(selectionZoneText));

        if(System.currentTimeMillis() - lastBlinkTimeMs >= cursor.getBlinkSpeedMs()){
            cursorBlinkState++;
            cursorBlinkState = cursorBlinkState % 4;
            lastBlinkTimeMs = System.currentTimeMillis();
        }
    }

    /**
     * Clears the screen.
     */
    protected void clearScreen() {
        robot.telemetry.clearAll();
        robot.telemetry.update();
    }

    /**
     * Adds a single line to the screen.
     *
     * @param line - The line object to display.
     * @param lineNumber - The line number (starts at 0 at the top).
     */
    protected void displayLine(GuiLine line, int lineNumber){
        if(cursor.getY() == lineNumber){
            blinkCursor(line);
        }
        else {
            robot.telemetry.addLine(line.getLineText());
        }
    }
}
