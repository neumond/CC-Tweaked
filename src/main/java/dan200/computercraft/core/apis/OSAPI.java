/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.shared.util.StringUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

import static dan200.computercraft.api.lua.LuaValues.checkFinite;

/**
 * The {@link OSAPI} API supports
 *
 * @cc.api os
 */
public class OSAPI implements ILuaAPI
{
    private final IAPIEnvironment apiEnvironment;

    private final Int2ObjectMap<Alarm> m_alarms = new Int2ObjectOpenHashMap<>();
    private int m_clock;
    private double m_time;
    private int m_day;

    private int m_nextAlarmToken = 0;

    private static class Alarm implements Comparable<Alarm>
    {
        final double m_time;
        final int m_day;

        Alarm( double time, int day )
        {
            m_time = time;
            m_day = day;
        }

        @Override
        public int compareTo( @Nonnull Alarm o )
        {
            double t = m_day * 24.0 + m_time;
            double ot = m_day * 24.0 + m_time;
            return Double.compare( t, ot );
        }
    }

    public OSAPI( IAPIEnvironment environment )
    {
        apiEnvironment = environment;
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "os" };
    }

    @Override
    public void startup()
    {
        m_time = apiEnvironment.getComputerEnvironment().getTimeOfDay();
        m_day = apiEnvironment.getComputerEnvironment().getDay();
        m_clock = 0;

        synchronized( m_alarms )
        {
            m_alarms.clear();
        }
    }

    @Override
    public void update()
    {
        m_clock++;

        // Wait for all of our alarms
        synchronized( m_alarms )
        {
            double previousTime = m_time;
            int previousDay = m_day;
            double time = apiEnvironment.getComputerEnvironment().getTimeOfDay();
            int day = apiEnvironment.getComputerEnvironment().getDay();

            if( time > previousTime || day > previousDay )
            {
                double now = m_day * 24.0 + m_time;
                Iterator<Int2ObjectMap.Entry<Alarm>> it = m_alarms.int2ObjectEntrySet().iterator();
                while( it.hasNext() )
                {
                    Int2ObjectMap.Entry<Alarm> entry = it.next();
                    Alarm alarm = entry.getValue();
                    double t = alarm.m_day * 24.0 + alarm.m_time;
                    if( now >= t )
                    {
                        apiEnvironment.queueEvent( "alarm", entry.getIntKey() );
                        it.remove();
                    }
                }
            }

            m_time = time;
            m_day = day;
        }
    }

    @Override
    public void shutdown()
    {
        synchronized( m_alarms )
        {
            m_alarms.clear();
        }
    }

    private static float getTimeForCalendar( Calendar c )
    {
        float time = c.get( Calendar.HOUR_OF_DAY );
        time += c.get( Calendar.MINUTE ) / 60.0f;
        time += c.get( Calendar.SECOND ) / (60.0f * 60.0f);
        return time;
    }

    private static int getDayForCalendar( Calendar c )
    {
        GregorianCalendar g = c instanceof GregorianCalendar ? (GregorianCalendar) c : new GregorianCalendar();
        int year = c.get( Calendar.YEAR );
        int day = 0;
        for( int y = 1970; y < year; y++ )
        {
            day += g.isLeapYear( y ) ? 366 : 365;
        }
        day += c.get( Calendar.DAY_OF_YEAR );
        return day;
    }

    private static long getEpochForCalendar( Calendar c )
    {
        return c.getTime().getTime();
    }

    @LuaFunction
    public final void queueEvent( String name, IArguments args )
    {
        apiEnvironment.queueEvent( name, args.drop( 1 ).getAll() );
    }

    @LuaFunction
    public final int startTimer( double timer ) throws LuaException
    {
        return apiEnvironment.startTimer( Math.round( checkFinite( 0, timer ) / 0.05 ) );
    }

    @LuaFunction
    public final void cancelTimer( int token )
    {
        apiEnvironment.cancelTimer( token );
    }

    @LuaFunction
    public final int setAlarm( double time ) throws LuaException
    {
        checkFinite( 0, time );
        if( time < 0.0 || time >= 24.0 ) throw new LuaException( "Number out of range" );
        synchronized( m_alarms )
        {
            int day = time > m_time ? m_day : m_day + 1;
            m_alarms.put( m_nextAlarmToken, new Alarm( time, day ) );
            return m_nextAlarmToken++;
        }
    }

    @LuaFunction
    public final void cancelAlarm( int token )
    {
        synchronized( m_alarms )
        {
            m_alarms.remove( token );
        }
    }

    @LuaFunction( "shutdown" )
    public final void doShutdown()
    {
        apiEnvironment.shutdown();
    }

    @LuaFunction( "reboot" )
    public final void doReboot()
    {
        apiEnvironment.reboot();
    }

    @LuaFunction( { "getComputerID", "computerID" } )
    public final int getComputerID()
    {
        return apiEnvironment.getComputerID();
    }

    @LuaFunction( { "getComputerLabel", "computerLabel" } )
    public final Object[] getComputerLabel()
    {
        String label = apiEnvironment.getLabel();
        return label == null ? null : new Object[] { label };
    }

    /**
     * Set the label of this computer.
     *
     * @param label The new label. May be {@code nil} in order to clear it.
     */
    @LuaFunction
    public final void setComputerLabel( Optional<String> label )
    {
        apiEnvironment.setLabel( StringUtil.normaliseLabel( label.orElse( null ) ) );
    }

    @LuaFunction
    public final double clock()
    {
        return m_clock * 0.05;
    }

    @LuaFunction
    public final Object time( IArguments args ) throws LuaException
    {
        Object value = args.get( 0 );
        if( value instanceof Map ) return LuaDateTime.fromTable( (Map<?, ?>) value );

        String param = args.optString( 0, "ingame" );
        switch( param.toLowerCase( Locale.ROOT ) )
        {
            case "utc": // Get Hour of day (UTC)
                return getTimeForCalendar( Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
            case "local": // Get Hour of day (local time)
                return getTimeForCalendar( Calendar.getInstance() );
            case "ingame": // Get in-game hour
                return m_time;
            default:
                throw new LuaException( "Unsupported operation" );
        }
    }

    @LuaFunction
    public final int day( Optional<String> args ) throws LuaException
    {
        switch( args.orElse( "ingame" ).toLowerCase( Locale.ROOT ) )
        {
            case "utc":     // Get numbers of days since 1970-01-01 (utc)
                return getDayForCalendar( Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) ) );
            case "local": // Get numbers of days since 1970-01-01 (local time)
                return getDayForCalendar( Calendar.getInstance() );
            case "ingame":// Get game day
                return m_day;
            default:
                throw new LuaException( "Unsupported operation" );
        }
    }

    @LuaFunction
    public final long epoch( Optional<String> args ) throws LuaException
    {
        switch( args.orElse( "ingame" ).toLowerCase( Locale.ROOT ) )
        {
            case "utc":
            {
                // Get utc epoch
                Calendar c = Calendar.getInstance( TimeZone.getTimeZone( "UTC" ) );
                return getEpochForCalendar( c );
            }
            case "local":
            {
                // Get local epoch
                Calendar c = Calendar.getInstance();
                return getEpochForCalendar( c );
            }
            case "ingame":
                // Get in-game epoch
                synchronized( m_alarms )
                {
                    return m_day * 86400000 + (int) (m_time * 3600000.0f);
                }
            default:
                throw new LuaException( "Unsupported operation" );
        }
    }

    @LuaFunction
    public final Object date( Optional<String> formatA, Optional<Long> timeA ) throws LuaException
    {
        String format = formatA.orElse( "%c" );
        long time = timeA.orElseGet( () -> Instant.now().getEpochSecond() );

        Instant instant = Instant.ofEpochSecond( time );
        ZonedDateTime date;
        ZoneOffset offset;
        if( format.startsWith( "!" ) )
        {
            offset = ZoneOffset.UTC;
            date = ZonedDateTime.ofInstant( instant, offset );
            format = format.substring( 1 );
        }
        else
        {
            ZoneId id = ZoneId.systemDefault();
            offset = id.getRules().getOffset( instant );
            date = ZonedDateTime.ofInstant( instant, id );
        }

        if( format.equals( "*t" ) ) return LuaDateTime.toTable( date, offset, instant );

        DateTimeFormatterBuilder formatter = new DateTimeFormatterBuilder();
        LuaDateTime.format( formatter, format, offset );
        return formatter.toFormatter( Locale.ROOT ).format( date );
    }

}
