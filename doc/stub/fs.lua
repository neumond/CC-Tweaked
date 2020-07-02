---  The FS API allows you to manipulate files and the filesystem.
--
-- @module fs

-- Defined in bios.lua
function complete(sPath, sLocation, bIncludeFiles, bIncludeDirs) end

--- A file handle which can be read from.
--
-- @type ReadHandle
-- @see fs.open
local ReadHandle = {}
function ReadHandle.read(count) end
function ReadHandle.readAll() end
function ReadHandle.readLine(with_trailing) end
function ReadHandle.seek(whence, offset) end
function ReadHandle.close() end

--- A file handle which can be written to.
--
-- @type WriteHandle
-- @see fs.open
local WriteHandle = {}
function WriteHandle.write(text) end
function WriteHandle.writeLine(text) end
function WriteHandle.flush(text) end
function WriteHandle.seek(whence, offset) end
function WriteHandle.close() end
