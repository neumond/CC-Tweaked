/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.processor;

import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.*;
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.tools.Diagnostic;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cc.tweaked.processor.Helpers.is;

public class TypeConverter extends SimpleTypeVisitor8<StringBuilder, StringBuilder>
{
    private final Environment env;
    private final Element element;

    public TypeConverter( Environment env, Element element )
    {
        this.env = env;
        this.element = element;
    }

    public static String of( Environment env, Element element, TypeMirror mirror )
    {
        return new TypeConverter( env, element ).visit( mirror, new StringBuilder() ).toString();
    }

    @Override
    public StringBuilder visitUnion( UnionType t, StringBuilder stringBuilder )
    {
        boolean first = true;
        for( TypeMirror mirror : t.getAlternatives() )
        {
            visit( mirror, stringBuilder );

            if( first ) stringBuilder.append( "|" );
            first = false;
        }
        return stringBuilder;
    }

    @Override
    public StringBuilder visitWildcard( WildcardType t, StringBuilder stringBuilder )
    {
        return stringBuilder.append( "any" );
    }

    @Override
    public StringBuilder visitPrimitive( PrimitiveType t, StringBuilder stringBuilder )
    {
        switch( t.getKind() )
        {
            case DOUBLE:
            case INT:
            case LONG:
                return stringBuilder.append( "number" );
            case BOOLEAN:
                return stringBuilder.append( "boolean" );
            default:
                return super.visitPrimitive( t, stringBuilder );
        }
    }

    @Override
    public StringBuilder visitArray( ArrayType t, StringBuilder stringBuilder )
    {
        if( MoreTypes.isTypeOf( Object.class, t.getComponentType() ) ) return stringBuilder.append( "any..." );

        stringBuilder.append( "{ " );
        visit( t.getComponentType(), stringBuilder );
        return stringBuilder.append( "... }" );
    }

    @Override
    public StringBuilder visitDeclared( DeclaredType t, StringBuilder stringBuilder )
    {
        TypeElement type = MoreElements.asType( t.asElement() );
        if( is( type, String.class ) || is( type, ByteBuffer.class ) || is( type.getSuperclass(), Enum.class ) )
        {
            return stringBuilder.append( "string" );
        }
        else if( is( type, Integer.class ) || is( type, Double.class ) || is( type, Long.class ) )
        {
            return stringBuilder.append( "number" );
        }
        else if( is( type, Boolean.class ) )
        {
            return stringBuilder.append( "boolean" );
        }
        else if( is( type, Object.class ) )
        {
            return stringBuilder.append( "any" );
        }
        else if( is( type, Map.class ) )
        {
            // TODO: Better handling
            return stringBuilder.append( "table" );
        }
        else if( is( type, List.class ) || is( type, Collection.class ) ) // TODO: Subclass instead!
        {
            // TODO: Better handling
            return stringBuilder.append( "table" );
        }
        else if( is( type, "dan200.computercraft.api.lua.MethodResult" ) )
        {
            return stringBuilder.append( "any..." );
        }
        else
        {
            return defaultAction( t, stringBuilder );
        }
    }

    @Override
    protected StringBuilder defaultAction( TypeMirror e, StringBuilder stringBuilder )
    {
        env.message( Diagnostic.Kind.ERROR, "Cannot handle type " + e, element );
        return stringBuilder;
    }
}
