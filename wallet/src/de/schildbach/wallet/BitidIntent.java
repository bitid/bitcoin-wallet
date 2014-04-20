/*
 * Copyright 2014 the original author or authors.
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

package de.schildbach.wallet;

import java.net.URI;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import android.os.Parcel;
import android.os.Parcelable;
import de.schildbach.wallet.util.Bitid;

/**
 * @author Eric Larchevêque
 */
public final class BitidIntent implements Parcelable
{
	@CheckForNull
	public final String message;

	@CheckForNull
	public final URI callback;

	@CheckForNull
	public final String nonce;
	
	public BitidIntent(@Nullable final String message, @Nullable final URI callback, @Nullable final String nonce)
	{
	    this.message = message;
	    this.callback = callback;
	    this.nonce = nonce;
	}
	
	public static BitidIntent fromBitidUri(@Nonnull final URI bitidUri)
	{
	    return new BitidIntent(bitidUri.toString(), Bitid.buildCallbackUriFromBitidUri(bitidUri), Bitid.extractNonceFromBitidUri(bitidUri));
	}
	
	public String getHost()
	{
	    return callback.getHost();
	}
	
	public String getAddressLabel()
	{
	    return "bitid:" + callback.toString() + "*";
	}
	
	@Override
	public String toString()
	{
		final StringBuilder builder = new StringBuilder();

		builder.append(getClass().getSimpleName());
		builder.append('[');
		builder.append(message);
		builder.append(',');
		builder.append(callback);
		builder.append(',');
		builder.append(nonce);
		builder.append(']');

		return builder.toString();
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags)
	{
		dest.writeString(message);
		dest.writeSerializable(callback);
		dest.writeString(nonce);
	}

	public static final Parcelable.Creator<BitidIntent> CREATOR = new Parcelable.Creator<BitidIntent>()
	{
		@Override
		public BitidIntent createFromParcel(final Parcel in)
		{
			return new BitidIntent(in);
		}

		@Override
		public BitidIntent[] newArray(final int size)
		{
			return new BitidIntent[size];
		}
	};

	private BitidIntent(final Parcel in)
	{
	    message = in.readString();
	    callback = (URI) in.readSerializable();
	    nonce = in.readString();
	}

}
