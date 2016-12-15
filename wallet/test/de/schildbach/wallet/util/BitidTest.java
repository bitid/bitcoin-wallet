/*
 * Copyright 2013-2014 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.schildbach.wallet.util;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Test;

/**
 * @author Eric Larchevêque
 */
public class BitidTest
{
	@Test
	public void checkBitidUriValidity() throws Exception
	{
        assertEquals(true, Bitid.checkBitidUriValidity(new URI("bitid://bitid.bitcoin.blue/callback?x=295e882efa62f9e0")));

	    assertEquals(true, Bitid.checkBitidUriValidity(new URI("bitid://bitid.bitcoin.blue/callback?x=295e882efa62f9e0&u=1")));

	    assertEquals(false, Bitid.checkBitidUriValidity(new URI("bitid://bitid.bitcoin.blue/?x=295e882efa62f9e0")));

        assertEquals(false, Bitid.checkBitidUriValidity(new URI("bitid://bitid.bitcoin.blue?x=295e882efa62f9e0")));

	    assertEquals(false, Bitid.checkBitidUriValidity(new URI("bitid://bitid.bitcoin.blue/callback?z=295e882efa62f9e0")));

        assertEquals(false, Bitid.checkBitidUriValidity(new URI("http://bitid.bitcoin.blue/callback?x=295e882efa62f9e0")));
	}
	
	@Test
	public void extractNonceFromBitidUri() throws Exception
	{
	    assertEquals("295e882efa62f9e0", Bitid.extractNonceFromBitidUri(new URI("bitid://bitid.bitcoin.blue/callback?x=295e882efa62f9e0")));
	}
	
	@Test
	public void buildCallbackUriFromBitidUri() throws Exception
	{
	    assertEquals("https://bitid.bitcoin.blue/callback", Bitid.buildCallbackUriFromBitidUri(new URI("bitid://bitid.bitcoin.blue/callback?x=295e882efa62f9e0")).toString());

	    assertEquals("http://bitid.bitcoin.blue/callback", Bitid.buildCallbackUriFromBitidUri(new URI("bitid://bitid.bitcoin.blue/callback?x=295e882efa62f9e0&u=1")).toString());

        assertEquals("http://bitid.bitcoin.blue:3000/callback", Bitid.buildCallbackUriFromBitidUri(new URI("bitid://bitid.bitcoin.blue:3000/callback?x=295e882efa62f9e0&u=1")).toString());
    }
}
