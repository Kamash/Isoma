/*
 * Copyright (C) 2012 @ilabAfrica
 * 
 * This file is part of Isoma
 *
 * Isoma is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Isoma is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Isoma.  If not, see <http://www.gnu.org/licenses/>.*
 */
package com.android.isoma;

import com.android.isoma.library.LibraryService;
import com.android.isoma.library.SqlLiteLibraryService;
import com.android.isoma.sync.PageTurnerWebProgressService;
import com.android.isoma.sync.ProgressService;
import roboguice.config.AbstractAndroidModule;

public class IsomaModule extends AbstractAndroidModule {

	@Override
	protected void configure() {
		
		bind( LibraryService.class ).to( SqlLiteLibraryService.class );
		bind( ProgressService.class ).to( PageTurnerWebProgressService.class );
		
	}
}
