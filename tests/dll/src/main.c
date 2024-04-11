/*
 * Copyright (c) 2024 Broadcom.
 * The term "Broadcom" refers to Broadcom Inc. and/or its subsidiaries.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Broadcom, Inc. - initial API and implementation
 *
 */

#include <stdio.h>
#include <stdlib.h>

#include "libcobol_ls_server.h"

int main(int argc, char **argv)
{
    graal_isolate_t* isolate = NULL;
    graal_isolatethread_t* thread = NULL;

    if (graal_create_isolate(NULL, &isolate, &thread) != 0) {
        fprintf(stderr, "cobol_parser_wrapper_main: graal_create_isolate error\n");
        return 1;
    }

    int retVal = nativeImageMain(thread, argc, argv);

    graal_tear_down_isolate(thread);

    return retVal;
}
