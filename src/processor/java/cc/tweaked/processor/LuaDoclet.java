/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.processor;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class LuaDoclet implements Doclet
{
    public static final String LUA_FUNCTION = "dan200.computercraft.api.lua.LuaFunction";
    public static final String PERIPHERAL = "dan200.computercraft.api.peripheral.IPeripheral";
    public static final String LUA_API = "dan200.computercraft.api.lua.ILuaAPI";

    private String output;
    private Locale locale;
    private Reporter reporter;

    private final Set<Option> options = Set.of(
        new BasicOption( "-d", "Set the output directory", "FILE", o -> output = o ),
        new BasicOption( "-doctitle", "Title for the overview page", "TITLE" ),
        new BasicOption( "-windowtitle", "The title of the documentation", "TITLE" )
    );

    @Override
    public void init( Locale locale, Reporter reporter )
    {
        this.locale = locale;
        this.reporter = reporter;
    }

    @Override
    public String getName()
    {
        return "LuaDoclet";
    }

    @Override
    public Set<? extends Option> getSupportedOptions()
    {
        return options;
    }

    @Override
    public SourceVersion getSupportedSourceVersion()
    {
        return SourceVersion.RELEASE_8;
    }

    @Override
    public boolean run( DocletEnvironment docEnv )
    {
        Environment env = Environment.of( docEnv, reporter );
        if( env == null ) return false;

        List<MethodData> methods = docEnv.getSpecifiedElements().stream()
            .filter( x -> x.getKind() == ElementKind.CLASS ).map( TypeElement.class::cast )
            .flatMap( x -> x.getEnclosedElements().stream() )

            // Only allow instance methods. Static methods are "generic peripheral" ones, and so are unsuitable.
            .filter( x -> x.getKind() == ElementKind.METHOD ).map( ExecutableElement.class::cast )
            .filter( x -> !x.getModifiers().contains( Modifier.STATIC ) )

            // Now find actual @LuaFunction methods!
            .map( element -> {
                AnnotationMirror mirror = Helpers.getAnnotation( element, env.getLuaFunction() );
                if( mirror == null ) return null;

                return MethodData.of( env, element, mirror );
            } ).filter( Objects::nonNull )
            .collect( Collectors.toList() );

        return true;
    }

    private static class BasicOption implements Option
    {
        private final String name;
        private final String description;
        private final String parameter;
        private final Consumer<String> process;

        private BasicOption( String name, String description, String parameter, Consumer<String> process )
        {
            this.name = name;
            this.description = description;
            this.parameter = parameter;
            this.process = process;
        }

        private BasicOption( String name, String description, String parameter )
        {
            this( name, description, parameter, x -> {} );
        }

        @Override
        public int getArgumentCount()
        {
            return 1;
        }

        @Override
        public String getDescription()
        {
            return description;
        }

        @Override
        public Kind getKind()
        {
            return Kind.STANDARD;
        }

        @Override
        public List<String> getNames()
        {
            return Collections.singletonList( name );
        }

        @Override
        public String getParameters()
        {
            return parameter;
        }

        @Override
        public boolean process( String option, List<String> arguments )
        {
            if( arguments.size() != 1 ) return false;
            process.accept( arguments.get( 0 ) );
            return true;
        }
    }
}
