/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package cc.tweaked.processor;

import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.LiteralTree;
import com.sun.source.doctree.TextTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.SimpleDocTreeVisitor;

import javax.annotation.Nonnull;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import java.util.function.BiConsumer;

public class DocConverter extends SimpleDocTreeVisitor<StringBuilder, StringBuilder>
{
    private final BiConsumer<DocTree, String> error;

    public static String of( Environment env, Element owner, DocTree tree )
    {
        return makeDocConverter( env, owner ).visit( tree, new StringBuilder() ).toString();
    }

    public static String of( Environment env, Element owner, Iterable<? extends DocTree> tree )
    {
        return makeDocConverter( env, owner ).visit( tree, new StringBuilder() ).toString();
    }

    @Nonnull
    private static DocConverter makeDocConverter( Environment env, Element owner )
    {
        return new DocConverter( ( t, m ) -> {
            DocTrees trees = env.trees();
            env.trees().printMessage(
                Diagnostic.Kind.ERROR, m, t,
                trees.getDocCommentTree( owner ), trees.getPath( owner ).getCompilationUnit()
            );
        } );
    }

    private DocConverter( BiConsumer<DocTree, String> error )
    {
        this.error = error;
    }

    @Override
    public StringBuilder visitText( TextTree node, StringBuilder stringBuilder )
    {
        return stringBuilder.append( node.getBody() );
    }

    @Override
    public StringBuilder visitLiteral( LiteralTree node, StringBuilder stringBuilder )
    {
        String body = node.getBody().getBody();
        switch( body )
        {
            case "nil":
            case "true":
            case "false":
                return stringBuilder.append( "@{" ).append( body ).append( "}" );
            default:
                return stringBuilder.append( "`" ).append( body ).append( "`" );
        }
    }

    @Override
    protected StringBuilder defaultAction( DocTree node, StringBuilder stringBuilder )
    {
        error.accept( node, "Visiting unknown node " + node.getKind() );
        return stringBuilder;
    }
}
