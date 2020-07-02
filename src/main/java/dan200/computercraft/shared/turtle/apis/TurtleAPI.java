/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.apis;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import dan200.computercraft.api.turtle.event.TurtleInspectItemEvent;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.asm.TaskCallback;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.peripheral.generic.data.ItemData;
import dan200.computercraft.shared.turtle.core.*;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @cc.module turtle
 */
public class TurtleAPI implements ILuaAPI
{
    private final IAPIEnvironment environment;
    private final ITurtleAccess turtle;

    public TurtleAPI( IAPIEnvironment environment, ITurtleAccess turtle )
    {
        this.environment = environment;
        this.turtle = turtle;
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "turtle" };
    }

    private MethodResult trackCommand( ITurtleCommand command )
    {
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return turtle.executeCommand( command );
    }

    /**
     * Move the turtle forward one block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether the turtle could successfully move.
     * @cc.treturn string|nil The reason the turtle could not move.
     */
    @LuaFunction
    public final MethodResult forward()
    {
        return trackCommand( new TurtleMoveCommand( MoveDirection.FORWARD ) );
    }

    /**
     * Move the turtle backwards one block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether the turtle could successfully move.
     * @cc.treturn string|nil The reason the turtle could not move.
     */
    @LuaFunction
    public final MethodResult back()
    {
        return trackCommand( new TurtleMoveCommand( MoveDirection.BACK ) );
    }

    /**
     * Move the turtle up one block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether the turtle could successfully move.
     * @cc.treturn string|nil The reason the turtle could not move.
     */
    @LuaFunction
    public final MethodResult up()
    {
        return trackCommand( new TurtleMoveCommand( MoveDirection.UP ) );
    }

    /**
     * Move the turtle down one block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean Whether the turtle could successfully move.
     * @cc.treturn string|nil The reason the turtle could not move.
     */
    @LuaFunction
    public final MethodResult down()
    {
        return trackCommand( new TurtleMoveCommand( MoveDirection.DOWN ) );
    }

    /**
     * Rotate the turtle 90 degress to the left.
     *
     * @return The turtle command result.
     */
    @LuaFunction
    public final MethodResult turnLeft()
    {
        return trackCommand( new TurtleTurnCommand( TurnDirection.LEFT ) );
    }

    /**
     * Rotate the turtle 90 degress to the right.
     *
     * @return The turtle command result.
     */
    @LuaFunction
    public final MethodResult turnRight()
    {
        return trackCommand( new TurtleTurnCommand( TurnDirection.RIGHT ) );
    }

    /**
     * Attempt to break the block in front of the turtle.
     *
     * This requires a turtle tool capable of breaking the block. Diamond pickaxes
     * (mining turtles) can break any vanilla block, but other tools (such as axes)
     * are more limited.
     *
     * @param side The specific tool to use. Should be "left" or "right".
     * @return The turtle command result.
     * @cc.treturn boolean Whether a block was broken.
     * @cc.treturn string|nil The reason no block was broken.
     */
    @LuaFunction
    public final MethodResult dig( Optional<TurtleSide> side )
    {
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( TurtleToolCommand.dig( InteractDirection.FORWARD, side.orElse( null ) ) );
    }

    /**
     * Attempt to break the block above the turtle. See {@link #dig} for full details.
     *
     * @param side The specific tool to use.
     * @return The turtle command result.
     * @cc.treturn boolean Whether a block was broken.
     * @cc.treturn string|nil The reason no block was broken.
     */
    @LuaFunction
    public final MethodResult digUp( Optional<TurtleSide> side )
    {
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( TurtleToolCommand.dig( InteractDirection.UP, side.orElse( null ) ) );
    }

    /**
     * Attempt to break the block below the turtle. See {@link #dig} for full details.
     *
     * @param side The specific tool to use.
     * @return The turtle command result.
     * @cc.treturn boolean Whether a block was broken.
     * @cc.treturn string|nil The reason no block was broken.
     */
    @LuaFunction
    public final MethodResult digDown( Optional<TurtleSide> side )
    {
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( TurtleToolCommand.dig( InteractDirection.DOWN, side.orElse( null ) ) );
    }

    /**
     * Place a block or item into the world in front of the turtle.
     *
     * @param args Arguments to place.
     * @return The turtle command result.
     * @cc.tparam [opt] string text When placing a sign, set its contents to this text.
     * @cc.treturn boolean Whether the block could be placed.
     * @cc.treturn string|nil The reason the block was not placed.
     */
    @LuaFunction
    public final MethodResult place( IArguments args )
    {
        return trackCommand( new TurtlePlaceCommand( InteractDirection.FORWARD, args.getAll() ) );
    }

    /**
     * Place a block or item into the world above the turtle.
     *
     * @param args Arguments to place.
     * @return The turtle command result.
     * @cc.tparam [opt] string text When placing a sign, set its contents to this text.
     * @cc.treturn boolean Whether the block could be placed.
     * @cc.treturn string|nil The reason the block was not placed.
     */
    @LuaFunction
    public final MethodResult placeUp( IArguments args )
    {
        return trackCommand( new TurtlePlaceCommand( InteractDirection.UP, args.getAll() ) );
    }

    /**
     * Place a block or item into the world below the turtle.
     *
     * @param args Arguments to place.
     * @return The turtle command result.
     * @cc.tparam [opt] string text When placing a sign, set its contents to this text.
     * @cc.treturn boolean Whether the block could be placed.
     * @cc.treturn string|nil The reason the block was not placed.
     */
    @LuaFunction
    public final MethodResult placeDown( IArguments args )
    {
        return trackCommand( new TurtlePlaceCommand( InteractDirection.DOWN, args.getAll() ) );
    }

    /**
     * Drop the currently selected stack into the inventory in front of the turtle, or as an item into the world if
     * there is no inventory.
     *
     * @param count The number of items to drop. If not given, the entire stack will be dropped.
     * @return The turtle command result.
     * @throws LuaException If dropping an invalid number of items.
     * @cc.treturn boolean Whether items were dropped.
     * @cc.treturn string|nil The reason the no items were dropped.
     * @see #select
     */
    @LuaFunction
    public final MethodResult drop( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleDropCommand( InteractDirection.FORWARD, checkCount( count ) ) );
    }

    /**
     * Drop the currently selected stack into the inventory above the turtle, or as an item into the world if there is
     * no inventory.
     *
     * @param count The number of items to drop. If not given, the entire stack will be dropped.
     * @return The turtle command result.
     * @throws LuaException If dropping an invalid number of items.
     * @cc.treturn boolean Whether items were dropped.
     * @cc.treturn string|nil The reason the no items were dropped.
     * @see #select
     */
    @LuaFunction
    public final MethodResult dropUp( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleDropCommand( InteractDirection.UP, checkCount( count ) ) );
    }

    /**
     * Drop the currently selected stack into the inventory in front of the turtle, or as an item into the world if
     * there is no inventory.
     *
     * @param count The number of items to drop. If not given, the entire stack will be dropped.
     * @return The turtle command result.
     * @throws LuaException If dropping an invalid number of items.
     * @cc.treturn boolean Whether items were dropped.
     * @cc.treturn string|nil The reason the no items were dropped.
     * @see #select
     */
    @LuaFunction
    public final MethodResult dropDown( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleDropCommand( InteractDirection.DOWN, checkCount( count ) ) );
    }

    /**
     * Change the currently selected slot.
     *
     * The selected slot is determines what slot actions like {@link #drop} or {@link #getItemCount} act on.
     *
     * @param slot The slot to select.
     * @throws LuaException If the slot is out of range.
     * @see #getSelectedSlot
     */

    @LuaFunction
    public final MethodResult select( int slot ) throws LuaException
    {
        int actualSlot = checkSlot( slot );
        return turtle.executeCommand( turtle -> {
            turtle.setSelectedSlot( actualSlot );
            return TurtleCommandResult.success();
        } );
    }

    /**
     * Get the number of items in the given slot.
     *
     * @param slot The slot we wish to check. Defaults to the {@link #select selected slot}.
     * @return The number of items in this slot.
     * @throws LuaException If the slot is out of range.
     */
    @LuaFunction
    public final int getItemCount( Optional<Integer> slot ) throws LuaException
    {
        int actualSlot = checkSlot( slot ).orElse( turtle.getSelectedSlot() );
        return turtle.getInventory().getStackInSlot( actualSlot ).getCount();
    }

    /**
     * Get the remaining number of items which may be stored in this stack.
     *
     * For instance, if a slot contains 13 blocks of dirt, it has room for another 51.
     *
     * @param slot The slot we wish to check. Defaults to the {@link #select selected slot}.
     * @return The space left in in this slot.
     * @throws LuaException If the slot is out of range.
     */
    @LuaFunction
    public final int getItemSpace( Optional<Integer> slot ) throws LuaException
    {
        int actualSlot = checkSlot( slot ).orElse( turtle.getSelectedSlot() );
        ItemStack stack = turtle.getInventory().getStackInSlot( actualSlot );
        return stack.isEmpty() ? 64 : Math.min( stack.getMaxStackSize(), 64 ) - stack.getCount();
    }

    /**
     * Check if there is a solid block in front of the turtle. In this case, solid refers to any non-air or liquid
     * block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean If there is a solid block in front.
     */
    @LuaFunction
    public final MethodResult detect()
    {
        return trackCommand( new TurtleDetectCommand( InteractDirection.FORWARD ) );
    }

    /**
     * Check if there is a solid block above the turtle. In this case, solid refers to any non-air or liquid block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean If there is a solid block in front.
     */
    @LuaFunction
    public final MethodResult detectUp()
    {
        return trackCommand( new TurtleDetectCommand( InteractDirection.UP ) );
    }

    /**
     * Check if there is a solid block below the turtle. In this case, solid refers to any non-air or liquid block.
     *
     * @return The turtle command result.
     * @cc.treturn boolean If there is a solid block in front.
     */
    @LuaFunction
    public final MethodResult detectDown()
    {
        return trackCommand( new TurtleDetectCommand( InteractDirection.DOWN ) );
    }

    @LuaFunction
    public final MethodResult compare()
    {
        return trackCommand( new TurtleCompareCommand( InteractDirection.FORWARD ) );
    }

    @LuaFunction
    public final MethodResult compareUp()
    {
        return trackCommand( new TurtleCompareCommand( InteractDirection.UP ) );
    }

    @LuaFunction
    public final MethodResult compareDown()
    {
        return trackCommand( new TurtleCompareCommand( InteractDirection.DOWN ) );
    }

    /**
     * Attack the entity in front of the turtle.
     *
     * @param side The specific tool to use.
     * @return The turtle command result.
     * @cc.treturn boolean Whether an entity was attacked.
     * @cc.treturn string|nil The reason nothing was attacked.
     */
    @LuaFunction
    public final MethodResult attack( Optional<TurtleSide> side )
    {
        return trackCommand( TurtleToolCommand.attack( InteractDirection.FORWARD, side.orElse( null ) ) );
    }

    /**
     * Attack the entity above the turtle.
     *
     * @param side The specific tool to use.
     * @return The turtle command result.
     * @cc.treturn boolean Whether an entity was attacked.
     * @cc.treturn string|nil The reason nothing was attacked.
     */
    @LuaFunction
    public final MethodResult attackUp( Optional<TurtleSide> side )
    {
        return trackCommand( TurtleToolCommand.attack( InteractDirection.UP, side.orElse( null ) ) );
    }

    /**
     * Attack the entity below the turtle.
     *
     * @param side The specific tool to use.
     * @return The turtle command result.
     * @cc.treturn boolean Whether an entity was attacked.
     * @cc.treturn string|nil The reason nothing was attacked.
     */
    @LuaFunction
    public final MethodResult attackDown( Optional<TurtleSide> side )
    {
        return trackCommand( TurtleToolCommand.attack( InteractDirection.DOWN, side.orElse( null ) ) );
    }

    /**
     * Suck an item from the inventory in front of the turtle, or from an item floating in the world.
     *
     * This will pull items into the first acceptable slot, starting at the {@link #select currently selected} one.
     *
     * @param count The number of items to suck. If not given, up to a stack of items will be picked up.
     * @return The turtle command result.
     * @throws LuaException If given an invalid number of items.
     * @cc.treturn boolean Whether items were picked up.
     * @cc.treturn string|nil The reason the no items were picked up.
     */
    @LuaFunction
    public final MethodResult suck( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleSuckCommand( InteractDirection.FORWARD, checkCount( count ) ) );
    }

    /**
     * Suck an item from the inventory above the turtle, or from an item floating in the world.
     *
     * @param count The number of items to suck. If not given, up to a stack of items will be picked up.
     * @return The turtle command result.
     * @throws LuaException If given an invalid number of items.
     * @cc.treturn boolean Whether items were picked up.
     * @cc.treturn string|nil The reason the no items were picked up.
     */
    @LuaFunction
    public final MethodResult suckUp( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleSuckCommand( InteractDirection.UP, checkCount( count ) ) );
    }

    /**
     * Suck an item from the inventory below the turtle, or from an item floating in the world.
     *
     * @param count The number of items to suck. If not given, up to a stack of items will be picked up.
     * @return The turtle command result.
     * @throws LuaException If given an invalid number of items.
     * @cc.treturn boolean Whether items were picked up.
     * @cc.treturn string|nil The reason the no items were picked up.
     */
    @LuaFunction
    public final MethodResult suckDown( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleSuckCommand( InteractDirection.DOWN, checkCount( count ) ) );
    }

    @LuaFunction
    public final Object getFuelLevel()
    {
        return turtle.isFuelNeeded() ? turtle.getFuelLevel() : "unlimited";
    }

    @LuaFunction
    public final MethodResult refuel( Optional<Integer> countA ) throws LuaException
    {
        int count = countA.orElse( Integer.MAX_VALUE );
        if( count < 0 ) throw new LuaException( "Refuel count " + count + " out of range" );
        return trackCommand( new TurtleRefuelCommand( count ) );
    }

    @LuaFunction
    public final MethodResult compareTo( int slot ) throws LuaException
    {
        return trackCommand( new TurtleCompareToCommand( checkSlot( slot ) ) );
    }

    @LuaFunction
    public final MethodResult transferTo( int slotArg, Optional<Integer> countArg ) throws LuaException
    {
        int slot = checkSlot( slotArg );
        int count = checkCount( countArg );
        return trackCommand( new TurtleTransferToCommand( slot, count ) );
    }

    /**
     * Get the currently sleected slot.
     *
     * @return The current slot.
     * @see #select
     */
    @LuaFunction
    public final int getSelectedSlot()
    {
        return turtle.getSelectedSlot() + 1;
    }

    @LuaFunction
    public final Object getFuelLimit()
    {
        return turtle.isFuelNeeded() ? turtle.getFuelLimit() : "unlimited";
    }

    @LuaFunction
    public final MethodResult equipLeft()
    {
        return trackCommand( new TurtleEquipCommand( TurtleSide.LEFT ) );
    }

    @LuaFunction
    public final MethodResult equipRight()
    {
        return trackCommand( new TurtleEquipCommand( TurtleSide.RIGHT ) );
    }

    @LuaFunction
    public final MethodResult inspect()
    {
        return trackCommand( new TurtleInspectCommand( InteractDirection.FORWARD ) );
    }

    @LuaFunction
    public final MethodResult inspectUp()
    {
        return trackCommand( new TurtleInspectCommand( InteractDirection.UP ) );
    }

    @LuaFunction
    public final MethodResult inspectDown()
    {
        return trackCommand( new TurtleInspectCommand( InteractDirection.DOWN ) );
    }

    /**
     * Get detailed information about the items in the given slot.
     *
     * @param context  The Lua context
     * @param slot     The slot to get information about. Defaults to the {@link #select selected slot}.
     * @param detailed Whether to include "detailed" information. When {@code true} the method will contain much
     *                 more information about the item at the cost of taking longer to run.
     * @return The command result.
     * @throws LuaException If the slot is out of range.
     * @cc.treturn nil|table Information about the given slot, or {@code nil} if it is empty.
     * @cc.usage Print the current slot, assuming it contains 13 dirt.
     *
     * <pre>{@code
     * print(textutils.serialize(turtle.getItemDetail()))
     * -- => {
     * --  name = "minecraft:dirt",
     * --  count = 13,
     * -- }
     * }</pre>
     */
    @LuaFunction
    public final MethodResult getItemDetail( ILuaContext context, Optional<Integer> slot, Optional<Boolean> detailed ) throws LuaException
    {
        int actualSlot = checkSlot( slot ).orElse( turtle.getSelectedSlot() );
        return detailed.orElse( false )
            ? TaskCallback.make( context, () -> getItemDetail( actualSlot, true ) )
            : MethodResult.of( getItemDetail( actualSlot, false ) );
    }

    private Object[] getItemDetail( int slot, boolean detailed )
    {
        ItemStack stack = turtle.getInventory().getStackInSlot( slot );
        if( stack.isEmpty() ) return new Object[] { null };

        Map<String, Object> table = detailed
            ? ItemData.fill( new HashMap<>(), stack )
            : ItemData.fillBasic( new HashMap<>(), stack );

        TurtleActionEvent event = new TurtleInspectItemEvent( turtle, stack, table, detailed );
        if( MinecraftForge.EVENT_BUS.post( event ) ) return new Object[] { false, event.getFailureMessage() };

        return new Object[] { table };
    }


    private static int checkSlot( int slot ) throws LuaException
    {
        if( slot < 1 || slot > 16 ) throw new LuaException( "Slot number " + slot + " out of range" );
        return slot - 1;
    }

    private static Optional<Integer> checkSlot( Optional<Integer> slot ) throws LuaException
    {
        return slot.isPresent() ? Optional.of( checkSlot( slot.get() ) ) : Optional.empty();
    }

    private static int checkCount( Optional<Integer> countArg ) throws LuaException
    {
        int count = countArg.orElse( 64 );
        if( count < 0 || count > 64 ) throw new LuaException( "Item count " + count + " out of range" );
        return count;
    }
}
