//******************************************************************************
// Copyright (c) 2015 - 2018, The Regents of the University of California (Regents).
// All Rights Reserved. See LICENSE and LICENSE.SiFive for license details.
//------------------------------------------------------------------------------
// Author: Christopher Celio
//------------------------------------------------------------------------------

package boom.common

import chisel3._

import freechips.rocketchip.config.{Parameters, Config}
import freechips.rocketchip.subsystem.{SystemBusKey}
import freechips.rocketchip.devices.tilelink.{BootROMParams}
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._

import boom.bpu._
import boom.exu._
import boom.lsu._
import boom.system.BoomTilesKey

/**
 * Try to be a reasonable BOOM design point.
 */
class DefaultBoomConfig extends Config((site, here, up) => {

   // Top-Level
   case XLen => 64

   // Use this boot ROM for SimDTM.
   case BootROMParams => BootROMParams(contentFileName = "./rocket-chip/bootrom/bootrom.img")

   // Core Parameters
   case BoomTilesKey => up(BoomTilesKey, site) map { r => r.copy(
      core = r.core.copy(
         fetchWidth = 4,
         decodeWidth = 2,
         numRobEntries = 80,
         issueParams = Seq(
            IssueParams(issueWidth=1, numEntries=20, iqType=IQT_MEM.litValue),
            IssueParams(issueWidth=2, numEntries=20, iqType=IQT_INT.litValue),
            IssueParams(issueWidth=1, numEntries=20, iqType=IQT_FP.litValue)),
         numIntPhysRegisters = 100,
         numFpPhysRegisters = 64,
         numLsuEntries = 16,
         maxBrCount = 8,
         btb = BoomBTBParameters(nSets=512, nWays=4, nRAS=8, tagSz=13),
         enableBranchPredictor = true,
         tage = Some(TageParameters()),
         nPerfCounters = 29,
         fpu = Some(freechips.rocketchip.tile.FPUParams(sfmaLatency=4, dfmaLatency=4, divSqrt=true))),
      btb = Some(BTBParams(nEntries = 0, updatesOutOfOrder = true)),
      dcache = Some(DCacheParams(rowBits = site(SystemBusKey).beatBits, nSets=64, nWays=8, nMSHRs=4, nTLBEntries=16)),
      icache = Some(ICacheParams(fetchBytes = 4*4, rowBits = site(SystemBusKey).beatBits, nSets=64, nWays=8))
      )}
   // Set TL network to 128bits wide
   case SystemBusKey => up(SystemBusKey, site).copy(beatBytes = 16)
})

/**
 * Enables RV32 version of the core
 */
class WithBoomRV32 extends Config((site, here, up) => {
  case XLen => 32
  case BoomTilesKey => up(BoomTilesKey, site) map { r =>
    r.copy(core = r.core.copy(
      fpu = r.core.fpu.map(_.copy(fLen = 32)),
      mulDiv = Some(MulDivParams(mulUnroll = 8))))
  }
})

/**
 * Combines the Memory and Integer Issue Queues. Similar to BOOM v1.
 */
class WithUnifiedMemIntIQs extends Config((site, here, up) => {
   case BoomTilesKey => up(BoomTilesKey, site) map { r =>
      r.copy(core = r.core.copy(
         issueParams = r.core.issueParams.filter(_.iqType != IQT_MEM.litValue)
      ))
   }
})

/**
 * Remove FPU
 */
class WithoutBoomFPU extends Config((site, here, up) => {
   case BoomTilesKey => up(BoomTilesKey, site) map { r =>
      r.copy(core = r.core.copy(
         issueParams = r.core.issueParams.filter(_.iqType != IQT_FP.litValue),
         fpu = None))
   }
})

/**
 * Customize the amount of perf. counters (HPMs) for the core
 */
class WithNPerfCounters(n: Int) extends Config((site, here, up) => {
   case BoomTilesKey => up(BoomTilesKey, site) map { r => r.copy(core = r.core.copy(
      nPerfCounters = n
   ))}
})

/**
 * Enable tracing
 */
class WithTrace extends Config((site, here, up) => {
   case BoomTilesKey => up(BoomTilesKey, site) map { r => r.copy(trace = true) }
})

/**
 * Enable RVC
 */
class WithRVC extends Config((site, here, up) => {
   case BoomTilesKey => up(BoomTilesKey, site) map {r => r.copy(
      core = r.core.copy(
         fetchWidth = r.core.fetchWidth * 2,
         useCompressed = true))}
})

/**
 * Small BOOM! Try to be fast to compile and easier to debug.
 */
class WithSmallBooms extends Config((site, here, up) => {
   case BoomTilesKey => up(BoomTilesKey, site) map { r =>r.copy(
      core = r.core.copy(
         fetchWidth = 2,
         decodeWidth = 1,
         numRobEntries = 16,
         issueParams = Seq(
            IssueParams(issueWidth=1, numEntries=4, iqType=IQT_MEM.litValue),
            IssueParams(issueWidth=1, numEntries=4, iqType=IQT_INT.litValue),
            IssueParams(issueWidth=1, numEntries=4, iqType=IQT_FP.litValue)),
         numIntPhysRegisters = 48,
         numFpPhysRegisters = 48,
         numLsuEntries = 8,
         maxBrCount = 4,
         gshare = Some(GShareParameters(enabled=true, history_length=11, num_sets=2048)),
         nPerfCounters = 2),
      dcache = Some(DCacheParams(rowBits = site(SystemBusKey).beatBits, nSets=64, nWays=4, nMSHRs=2, nTLBEntries=8)),
      icache = Some(ICacheParams(rowBits = site(SystemBusKey).beatBits, nSets=64, nWays=4, fetchBytes=2*4))
      )}
   case SystemBusKey => up(SystemBusKey, site).copy(beatBytes = 8)
})

/**
 * Intermediate BOOM. Try to match the Cortex-A9.
 */
class WithMediumBooms extends Config((site, here, up) => {
   case BoomTilesKey => up(BoomTilesKey, site) map { r =>r.copy(
      core = r.core.copy(
         fetchWidth = 2,
         decodeWidth = 2,
         numRobEntries = 48,
         issueParams = Seq(
            IssueParams(issueWidth=1, numEntries=20, iqType=IQT_MEM.litValue),
            IssueParams(issueWidth=2, numEntries=16, iqType=IQT_INT.litValue),
            IssueParams(issueWidth=1, numEntries=10, iqType=IQT_FP.litValue)),
         numIntPhysRegisters = 70,
         numFpPhysRegisters = 64,
         numLsuEntries = 16,
         maxBrCount = 8,
         regreadLatency = 1,
         renameLatency = 2,
         btb = BoomBTBParameters(btbsa=true, nSets=64, nWays=2,
                                 nRAS=8, tagSz=20, bypassCalls=false, rasCheckForEmpty=false),
         gshare = Some(GShareParameters(enabled=true, history_length=23, num_sets=4096)),
         nPerfCounters = 6,
         fpu = Some(freechips.rocketchip.tile.FPUParams(sfmaLatency=4, dfmaLatency=4, divSqrt=true))),
      dcache = Some(DCacheParams(rowBits = site(SystemBusKey).beatBits, nSets=64, nWays=4, nMSHRs=2, nTLBEntries=8)),
      icache = Some(ICacheParams(rowBits = site(SystemBusKey).beatBits, nSets=64, nWays=4, fetchBytes=2*4))
      )}
   case SystemBusKey => up(SystemBusKey, site).copy(beatBytes = 8)
})

/**
 * Try to match the Cortex-A15. Don't expect good QoR (yet).
 */
class WithMegaBooms extends Config((site, here, up) => {
   case BoomTilesKey => up(BoomTilesKey, site) map { r =>r.copy(
      core = r.core.copy(
         fetchWidth = 8,
         decodeWidth = 4,
         numRobEntries = 128,
         issueParams = Seq(
            IssueParams(issueWidth=1, numEntries=20, iqType=IQT_MEM.litValue),
            IssueParams(issueWidth=2, numEntries=20, iqType=IQT_INT.litValue),
            IssueParams(issueWidth=1, numEntries=20, iqType=IQT_FP.litValue)),
         numIntPhysRegisters = 128,
         numFpPhysRegisters = 128,
         numLsuEntries = 32,
         maxBrCount = 16,
         btb = BoomBTBParameters(nSets=512, nWays=4, nRAS=16, tagSz=20),
         tage = Some(TageParameters())),
      dcache = Some(DCacheParams(rowBits = site(SystemBusKey).beatBytes*8,
                                 nSets=64, nWays=16, nMSHRs=8, nTLBEntries=32)),
      icache = Some(ICacheParams(fetchBytes = 8*4, rowBits = site(SystemBusKey).beatBytes*8, nSets=64, nWays=8))
      )}
   // Set TL network to 128bits wide
   case SystemBusKey => up(SystemBusKey, site).copy(beatBytes = 16)
})
