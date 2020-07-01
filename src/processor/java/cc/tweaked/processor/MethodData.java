/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.processor;

import com.google.auto.common.MoreTypes;
import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MethodData
{
    public final List<String> names;
    public final List<Argument> arguments;
    public final String returnType;

    public MethodData( List<String> names, List<Argument> arguments, String returnType )
    {
        this.names = Collections.unmodifiableList( names );
        this.arguments = Collections.unmodifiableList( arguments );
        this.returnType = returnType;
        System.out.println( String.format( "%s(%s) : %s", names, arguments, returnType ) );
    }

    @Nonnull
    public static MethodData of( Environment env, ExecutableElement method, AnnotationMirror mirror )
    {
        @SuppressWarnings( { "unchecked", "rawtypes" } )
        List<String> overrideNames = mirror == null ? null : (List) Helpers.getAnnotationValue( mirror, "value" );

        DocCommentTree tree = env.trees().getDocCommentTree( method );
        Map<String, List<? extends DocTree>> argumentDocs = tree == null ? Map.of() : tree.getBlockTags().stream()
            .filter( x -> x.getKind() == DocTree.Kind.PARAM ).map( ParamTree.class::cast )
            .collect( Collectors.toMap( x -> x.getName().getName().toString(), ParamTree::getDescription ) );

        List<Argument> arguments = method.getParameters().stream()
            .map( x -> Argument.of( env, argumentDocs, x ) )
            .filter( Objects::nonNull )
            .collect( Collectors.toList() );

        String returnType;
        if( method.getReturnType().getKind() == TypeKind.VOID )
        {
            returnType = null;
        }
        else
        {
            returnType = TypeConverter.of( env, method, method.getReturnType() );
            if( method.getAnnotation( Nullable.class ) != null ) returnType += "|nil";
        }

        return new MethodData( overrideNames == null
            ? Collections.singletonList( method.getSimpleName().toString() )
            : overrideNames, arguments, returnType );
    }

    public static class Argument
    {
        public final String name;
        public final boolean optional;
        @Nullable
        public final String type;
        @Nullable
        public final String description;

        private Argument( String name, boolean optional, @Nullable String type, @Nullable String description )
        {
            this.type = type;
            this.optional = optional;
            this.name = name;
            this.description = description;
        }

        @Nullable
        public static Argument of( Environment env, Map<String, List<? extends DocTree>> comments, VariableElement element )
        {
            String name = element.getSimpleName().toString();
            TypeMirror type = element.asType();
            List<? extends DocTree> commentList = comments.get( name );
            String description = commentList == null ? null : DocConverter.of( env, element.getEnclosingElement(), commentList );

            if( type.getKind() == TypeKind.DECLARED )
            {
                TypeElement argElem = MoreTypes.asTypeElement( type );
                if( Helpers.is( argElem, "dan200.computercraft.api.lua.IArguments" ) )
                {
                    return new Argument( "...", false, "any", description );
                }
                else if( Helpers.is( argElem, "dan200.computercraft.api.lua.ILuaContext" )
                    || Helpers.is( argElem, "dan200.computercraft.api.peripheral.IComputerAccess" ) )
                {
                    return null;
                }
            }

            TypeMirror optional = Helpers.unwrapOptional( type );
            String actualType = TypeConverter.of( env, element, optional == null ? type : optional );
            return new Argument( name, optional != null, actualType, description );
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            if( type == null )
            {
                builder.append( "@param " );
            }
            else
            {
                builder.append( "@tparam" ).append( optional ? "[opt] " : " " ).append( type );
            }

            builder.append( " " ).append( name );
            if( description != null ) builder.append( " " ).append( description );
            return builder.toString();
        }
    }
}
