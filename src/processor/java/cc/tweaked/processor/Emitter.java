/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.processor;

import com.sun.source.doctree.DocTree;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Emitter
{
    private final Map<ClassInfo, StringBuilder> modules = new HashMap<>();

    private final Environment env;
    private final Map<TypeElement, ClassInfo> types;

    public Emitter( Environment env, Map<ExecutableElement, MethodInfo> methods, Map<TypeElement, ClassInfo> types )
    {
        this.env = env;
        this.types = types;

        for( MethodInfo method : methods.values() ) methodBuilder( method );
    }

    @Nullable
    private ClassInfo resolveType( @Nullable Element type )
    {
        while( true )
        {
            if( type == null || type.getKind() != ElementKind.CLASS ) return null;
            TypeElement tyElem = (TypeElement) type;

            ClassInfo info = types.get( tyElem );
            if( info != null ) return info;

            type = env.types().asElement( tyElem.getSuperclass() );
        }
    }

    private StringBuilder classBuilder( @Nonnull ClassInfo info )
    {
        StringBuilder builder = new StringBuilder();

        builder.append( "--[[- " );
        new DocConverter( env, info.element() ).visit( info.doc(), builder );

        switch( info.kind() )
        {
            case PERIPHERAL:
                builder.append( "@module[kind=peripheral] " ).append( info.name() ).append( "\n]]\n" );
                break;
            case API:
                builder.append( "@module[module] " ).append( info.name() ).append( "\n]]\n" );
                break;
            case TYPE:
                builder.append( "@type " ).append( info.name() ).append( "\n]]\n" );
                builder.append( "local " ).append( info.name().replace( '.', '_' ) ).append( " = {}\n" );
                break;
        }

        return builder;
    }

    private void methodBuilder( @Nonnull MethodInfo info )
    {
        ExecutableElement method = info.element();
        ClassInfo klass = resolveType( method.getEnclosingElement() );
        if( klass == null )
        {
            // env.message( Diagnostic.Kind.NOTE, "Cannot find owner for " + info.name(), method );
            return;
        }

        StringBuilder builder = modules.computeIfAbsent( klass, this::classBuilder );

        DocConverter doc = new DocConverter( env, method );
        TypeConverter type = new TypeConverter( env, method );

        builder.append( "\n--[[- " );
        doc.visit( info.doc(), builder );
        builder.append( "\n" );

        String signature;
        if( !doc.hasParam() )
        {
            List<String> parameters = new ArrayList<>();
            for( VariableElement element : method.getParameters() )
            {
                String name = argBuilder( builder, doc, element );
                if( name != null ) parameters.add( name );
            }
            signature = String.join( ", ", parameters );
        }
        else
        {
            signature = "";
        }

        // If we've no explicit @cc.return annotation, then extract it from the @return tag.
        if( !doc.hasReturn() && method.getReturnType().getKind() != TypeKind.VOID )
        {
            if( Helpers.isAny( method.getReturnType() ) )
            {
                env.message( Diagnostic.Kind.WARNING, "Method returns an arbitrary object but has no @cc.return tag.", method );
            }

            builder.append( "@treturn " );
            type.visit( method.getReturnType(), builder );
            if( method.getAnnotation( Nullable.class ) != null ) builder.append( "|nil" );
            builder.append( " " );
            doc.visit( doc.getReturns(), builder );
            builder.append( "\n" );
        }

        // new DocConverter( env, method.element() ).visit( info.doc(), builder );
        builder.append( "]]\n" );

        builder.append( "function " ).append( info.name() ).append( "(" ).append( signature ).append( ") end\n" );
        for( String name : info.otherNames() )
        {
            builder.append( name ).append( " = " ).append( info.name() ).append( "\n" );
        }
    }

    private String argBuilder( StringBuilder builder, DocConverter docs, VariableElement element )
    {
        String name = element.getSimpleName().toString();
        TypeMirror type = element.asType();
        List<? extends DocTree> commentList = docs.getParams().get( name );
        // String description = commentList == null ? null : DocConverter.of( env, element.getEnclosingElement(), commentList );

        if( Helpers.is( type, "dan200.computercraft.api.lua.ILuaContext" )
            || Helpers.is( type, "dan200.computercraft.api.peripheral.IComputerAccess" ) )
        {
            return null;

        }
        if( Helpers.isAny( type ) )
        {
            env.message( Diagnostic.Kind.WARNING, "Method has a dynamic argument but has no @cc.param tag.", element );
            return "...";
        }

        TypeMirror optional = Helpers.unwrapOptional( type );

        builder.append( "@tparam" );
        if( optional != null ) builder.append( "[opt]" );
        builder.append( " " );

        new TypeConverter( env, element ).visit( optional == null ? type : optional, builder );
        builder.append( " " ).append( name ).append( " " );
        docs.visit( commentList, builder );
        builder.append( "\n" );

        return name;
    }

    public void emit( File output ) throws IOException
    {
        if( !output.exists() && !output.mkdirs() ) throw new IOException( "Cannot create output directory: " + output );

        for( Map.Entry<ClassInfo, StringBuilder> module : modules.entrySet() )
        {
            System.out.println( "Writing to " + new File( output, module.getKey().name() + ".lua" ) );
            try( Writer writer = new BufferedWriter( new FileWriter( new File( output, module.getKey().name() + ".lua" ), StandardCharsets.UTF_8 ) ) )
            {
                writer.write( module.getValue().toString() );
            }
        }
    }
}
